package com.chapeullah.cocclanbot.diagnostics;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.config.HttpConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class HttpDiagnosticsTest {
    private static final String TOKEN = "test-api-secret-that-must-never-be-logged";
    private final Logger logger = (Logger) LoggerFactory.getLogger(ClashOfClansClient.class);
    private ListAppender<ILoggingEvent> events;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        events = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(events);
        logger.setLevel(previousLevel);
        Thread.interrupted();
        MDC.clear();
    }

    @Test
    void timeoutWithNullMessageIsIdentifiedAndRecoveryUsesTheSameRequestId() {
        AtomicInteger attempts = new AtomicInteger();
        ClashOfClansClient client = client((uri, method) -> new MockClientHttpRequest(method, uri) {
            @Override
            protected ClientHttpResponse executeInternal() throws IOException {
                if (attempts.incrementAndGet() == 1) throw new HttpTimeoutException(null);
                return response("{\"items\":[]}", HttpStatus.OK);
            }
        });
        try (SynchronizationDiagnostics ignored = new SynchronizationDiagnostics("testSync", 55000)) {
            assertEquals("{\"items\":[]}", client.getCurrentRaid("#TEST"));
            var failure = event("HTTP request failed");
            var recovery = event("HTTP request recovered");
            assertTrue(failure.getFormattedMessage().contains("errorType=HTTP_TIMEOUT"));
            assertTrue(failure.getFormattedMessage().contains("ResourceAccessException -> HttpTimeoutException"));
            assertTrue(failure.getFormattedMessage().contains("responseHeadersReceived=false"));
            assertTrue(recovery.getFormattedMessage().contains("attempt=2/3"));
            assertEquals(failure.getMDCPropertyMap().get("requestId"), recovery.getMDCPropertyMap().get("requestId"));
            assertNotNull(failure.getMDCPropertyMap().get("syncId"));
            assertNull(MDC.get("requestId"));
            assertNull(SynchronizationDiagnostics.current().lastFailure());
        }
        assertEquals(2, attempts.get());
        assertNull(MDC.get("syncId"));
        assertNull(HttpRequestDiagnostics.current());
        assertNull(SynchronizationDiagnostics.current());
    }

    @Test
    void exhaustedRetriesPreserveTheOriginalExceptionAndLogTheLastAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        ClashOfClansClient client = client((uri, method) -> new MockClientHttpRequest(method, uri) {
            @Override
            protected ClientHttpResponse executeInternal() throws IOException {
                attempts.incrementAndGet();
                throw new HttpTimeoutException(null);
            }
        });
        ResourceAccessException exception = assertThrows(ResourceAccessException.class,
                () -> client.getCurrentRaid("#TEST"));
        assertInstanceOf(HttpTimeoutException.class, exception.getCause());
        assertEquals(3, attempts.get());
        assertTrue(events.list.stream().anyMatch(e -> e.getFormattedMessage().contains("attempt=3/3")
                && e.getFormattedMessage().contains("retrying=false")));
        assertNull(HttpRequestDiagnostics.current());
    }

    @Test
    void diagnosticSnippetIsBoundedAndMaskedWithoutChangingTheExceptionBody() {
        AtomicInteger attempts = new AtomicInteger();
        String body = "{\"reason\":\"maintenance\",\"token\":\"" + TOKEN + "\",\"message\":\"" + "x".repeat(4000) + "\"}";
        ClashOfClansClient client = client((uri, method) -> {
            attempts.incrementAndGet();
            var request = new MockClientHttpRequest(method, uri);
            var response = response(body, HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().set("Retry-After", "10");
            response.getHeaders().set("X-Request-ID", "api-request-123");
            response.getHeaders().set("Authorization", "Bearer " + TOKEN);
            request.setResponse(response);
            return request;
        });
        var exception = assertThrows(RestClientResponseException.class, () -> client.getCurrentRaid("#TEST"));
        String log = event("HTTP request failed").getFormattedMessage();
        assertTrue(log.contains("status=503"));
        assertTrue(log.contains("errorType=HTTP_STATUS"));
        assertTrue(log.contains("Retry-After=10"));
        assertTrue(log.contains("X-Request-ID=api-request-123"));
        assertTrue(log.contains("maintenance"));
        assertTrue(log.contains("<truncated>"));
        assertFalse(log.contains(TOKEN));
        assertFalse(log.contains("Authorization="));
        assertEquals(body, exception.getResponseBodyAsString());
        assertEquals(1, attempts.get());
    }

    @Test
    void failureDuringBodyReadKeepsTheReceivedStatusAndStage() {
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new InputStream() {
                private int reads;
                @Override
                public int read() throws IOException {
                    if (reads++ == 0) return '{';
                    throw new IOException("response stream closed");
                }
            }, HttpStatus.OK));
            return request;
        });
        assertThrows(RestClientException.class, () -> client.getCurrentRaid("#TEST"));
        String log = event("HTTP request failed").getFormattedMessage();
        assertTrue(log.contains("responseHeadersReceived=true"));
        assertTrue(log.contains("status=200"));
        assertTrue(log.contains("stage=READING_RESPONSE_BODY"));
        assertTrue(log.contains("errorType=RESPONSE_BODY_IO"));
        assertTrue(log.contains("responseBodyEndObserved=false"));
    }

    @Test
    void malformedJsonIsSeparatedFromTransportFailures() {
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            request.setResponse(response("{invalid", HttpStatus.OK));
            return request;
        });
        assertThrows(RestClientException.class, () -> client.getClan("#TEST"));
        assertTrue(event("HTTP request failed").getFormattedMessage().contains("errorType=RESPONSE_DECODING"));
        assertTrue(event("HTTP request failed").getFormattedMessage().contains("stage=DECODING_RESPONSE"));
    }

    @Test
    void failedErrorBodyReadRetainsTheObservedSanitizedPrefixAndOriginalCause() {
        IOException original = new IOException("truncated error response");
        byte[] prefix = ("{\"reason\":\"maintenance\",\"token\":\"" + TOKEN + "\"")
                .getBytes(StandardCharsets.UTF_8);
        AtomicInteger closes = new AtomicInteger();
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            var body = new SequenceInputStream(new ByteArrayInputStream(prefix), new InputStream() {
                @Override
                public int read() throws IOException { throw original; }
            });
            request.setResponse(new MockClientHttpResponse(body, HttpStatus.SERVICE_UNAVAILABLE) {
                @Override
                public void close() {
                    closes.incrementAndGet();
                    super.close();
                }
            });
            return request;
        });
        var exception = assertThrows(RestClientException.class, () -> client.getCurrentRaid("#TEST"));
        assertSame(original, exception.getCause());
        String log = event("HTTP request failed").getFormattedMessage();
        assertTrue(log.contains("errorType=RESPONSE_BODY_IO"), log);
        assertTrue(log.contains("status=503"), log);
        assertTrue(log.contains("maintenance"), log);
        assertFalse(log.contains(TOKEN), log);
        assertEquals(prefix.length, number(log, "responseBytesRead"));
        assertEquals(1, closes.get());
    }

    @Test
    void durationIncludesReadingTheBodyAfterHeadersWereReceived() {
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new ByteArrayInputStream("{\"items\":[]}".getBytes(StandardCharsets.UTF_8)) {
                private boolean delayed;
                private void delayOnce() {
                    if (!delayed) {
                        delayed = true;
                        try { Thread.sleep(50); } catch (InterruptedException e) { throw new IllegalStateException(e); }
                    }
                }
                @Override
                public synchronized int read() {
                    delayOnce();
                    return super.read();
                }
                @Override
                public synchronized int read(byte[] bytes, int offset, int length) {
                    delayOnce();
                    return super.read(bytes, offset, length);
                }
            }, HttpStatus.OK));
            return request;
        });
        client.getCurrentRaid("#TEST");
        String log = event("HTTP request succeeded").getFormattedMessage();
        assertTrue(number(log, "durationMs") >= 45);
        assertTrue(number(log, "headersDurationMs") < number(log, "durationMs"));
    }

    @Test
    void specificConnectTimeoutIsNotMisclassifiedAsGenericHttpTimeout() {
        assertEquals("CONNECT_TIMEOUT", HttpRequestDiagnostics.errorType(new HttpConnectTimeoutException(null), false, false));
        assertEquals("DNS_FAILURE", HttpRequestDiagnostics.errorType(new IOException(new UnknownHostException("host")), false, false));
        assertEquals("TLS_FAILURE", HttpRequestDiagnostics.errorType(new SSLHandshakeException("handshake"), false, false));
        assertEquals("HTTP_TIMEOUT", HttpRequestDiagnostics.errorType(new HttpTimeoutException(null), false, true));
        assertEquals("SYNC_DEADLINE", HttpRequestDiagnostics.errorType(new CancellationException(), false, true));
    }

    @Test
    void dnsAndTlsFailuresAreReportedFromTheActualRequestCauseChain() {
        for (IOException cause : new IOException[] {new UnknownHostException("fake.invalid"),
                new ConnectException("refused"), new SSLHandshakeException("test handshake")}) {
            events.list.clear();
            AtomicInteger attempts = new AtomicInteger();
            ClashOfClansClient client = client((uri, method) -> new MockClientHttpRequest(method, uri) {
                @Override
                protected ClientHttpResponse executeInternal() throws IOException {
                    if (attempts.incrementAndGet() == 1) throw cause;
                    return response("{}", HttpStatus.OK);
                }
            });
            assertEquals("{}", client.getCurrentRaid("#TEST"));
            String failure = event("HTTP request failed").getFormattedMessage();
            String expectedType = cause instanceof UnknownHostException ? "DNS_FAILURE"
                    : cause instanceof ConnectException ? "CONNECT_FAILURE" : "TLS_FAILURE";
            assertTrue(failure.contains("errorType=" + expectedType), failure);
            assertTrue(failure.contains("ResourceAccessException -> " + cause.getClass().getSimpleName()), failure);
            assertTrue(event("HTTP request recovered").getFormattedMessage().contains("errorType=none"));
        }
    }

    @Test
    void openingTheBodyCanFailAndStillClosesTheResponse() {
        AtomicInteger closes = new AtomicInteger();
        IOException original = new IOException("cannot open body");
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK) {
                @Override
                public InputStream getBody() throws IOException { throw original; }
                @Override
                public void close() { closes.incrementAndGet(); }
            });
            return request;
        });
        RestClientException failure = assertThrows(RestClientException.class, () -> client.getCurrentRaid("#TEST"));
        assertSame(original, failure.getCause());
        assertEquals(1, closes.get());
        String log = event("HTTP request failed").getFormattedMessage();
        assertTrue(log.contains("errorType=RESPONSE_BODY_IO"), log);
        assertTrue(log.contains("status=200"), log);
    }

    @Test
    void failureInspectingResponseMetadataClosesItAndPreservesBothErrors() {
        IllegalStateException original = new IllegalStateException("bad status metadata");
        IllegalStateException closeFailure = new IllegalStateException("close failed");
        AtomicInteger closes = new AtomicInteger();
        ClashOfClansClient client = client((uri, method) -> {
            var request = new MockClientHttpRequest(method, uri);
            request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK) {
                @Override
                public org.springframework.http.HttpStatusCode getStatusCode() { throw original; }
                @Override
                public void close() {
                    closes.incrementAndGet();
                    throw closeFailure;
                }
            });
            return request;
        });
        assertSame(original, assertThrows(IllegalStateException.class, () -> client.getCurrentRaid("#TEST")));
        assertEquals(1, closes.get());
        assertArrayEquals(new Throwable[] {closeFailure}, original.getSuppressed());
    }

    @Test
    void interruptDuringRetryDelayKeepsFailedAttemptDurationAndCause() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<HttpRequestDiagnostics> diagnostics = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Boolean> interrupted = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        HttpTimeoutException original = new HttpTimeoutException("fake timeout");
        ClashOfClansClient client = client((uri, method) -> new MockClientHttpRequest(method, uri) {
            @Override
            protected ClientHttpResponse executeInternal() throws IOException {
                diagnostics.set(HttpRequestDiagnostics.current());
                attempts.incrementAndGet();
                started.countDown();
                throw original;
            }
        });
        Thread worker = Thread.ofPlatform().start(() -> {
            try { client.getCurrentRaid("#TEST"); }
            catch (Throwable exception) { failure.set(exception); }
            finally { interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!diagnostics.get().summary().contains("stage=RETRY_DELAY") && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertTrue(diagnostics.get().summary().contains("stage=RETRY_DELAY"));
            Thread.sleep(50);
            worker.interrupt();
            worker.join(5000);
            assertFalse(worker.isAlive());
            assertEquals(1, attempts.get());
            assertEquals(Boolean.TRUE, interrupted.get());
            assertInstanceOf(InterruptedException.class, failure.get().getCause());
            assertSame(original, failure.get().getSuppressed()[0].getCause());
            String log = event("HTTP request retry interrupted").getFormattedMessage();
            assertTrue(number(log, "totalDurationMs") > number(log, "durationMs"), log);
            assertTrue(log.contains("errorType=REQUEST_CANCELLED"), log);
            long total = number(diagnostics.get().summary(), "totalDurationMs");
            Thread.sleep(10);
            assertEquals(total, number(diagnostics.get().summary(), "totalDurationMs"));
        } finally {
            worker.interrupt();
            worker.join(5000);
        }
    }

    @Test
    void alreadyInterruptedThreadNeverExecutesARequestAndKeepsItsFlag() {
        AtomicInteger attempts = new AtomicInteger();
        ClashOfClansClient client = client((uri, method) -> {
            attempts.incrementAndGet();
            throw new IOException("must not execute");
        });
        Thread.currentThread().interrupt();
        assertThrows(CancellationException.class, () -> client.getCurrentRaid("#TEST"));
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(0, attempts.get());
        assertNull(HttpRequestDiagnostics.current());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void sanitizerRemovesSecretsControlCharactersUserInfoAndQueryStrings() {
        DiagnosticSanitizer.registerSecret(TOKEN);
        String value = DiagnosticSanitizer.singleLine("Bearer another-secret\n" + TOKEN, 200);
        assertFalse(value.contains(TOKEN));
        assertFalse(value.contains("another-secret"));
        assertFalse(value.contains("\n"));
        assertEquals("https://clash.test/path", DiagnosticSanitizer.uri(
                java.net.URI.create("https://user:password@clash.test/path?token=secret#fragment")));
        assertFalse(DiagnosticSanitizer.singleLine(TOKEN.substring(0, 20), 200).contains(TOKEN.substring(0, 20)));
    }

    private ClashOfClansClient client(ClientHttpRequestFactory factory) {
        DiagnosticSanitizer.registerSecret(TOKEN);
        RestClient rest = HttpConfig.withDiagnostics(RestClient.builder()
                .baseUrl("https://clash.test").requestFactory(factory)).build();
        ClashOfClansClient client = new ClashOfClansClient(rest);
        ReflectionTestUtils.setField(client, "apiToken", TOKEN);
        return client;
    }

    private static MockClientHttpResponse response(String body, HttpStatus status) {
        var response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().set("Content-Type", "application/json");
        return response;
    }

    private ILoggingEvent event(String prefix) {
        return events.list.stream().filter(e -> e.getFormattedMessage().startsWith(prefix)).findFirst().orElseThrow();
    }

    private static long number(String message, String field) {
        var matcher = Pattern.compile("\\b" + field + "=(\\d+)").matcher(message);
        assertTrue(matcher.find(), message);
        return Long.parseLong(matcher.group(1));
    }
}
