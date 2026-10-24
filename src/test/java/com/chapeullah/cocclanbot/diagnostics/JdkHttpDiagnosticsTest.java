package com.chapeullah.cocclanbot.diagnostics;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.clan.dto.ClanResponse;
import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.config.HttpConfig;
import com.chapeullah.cocclanbot.config.Scheduler;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.telegram.notification.TelegramWarNotificationService;
import com.chapeullah.cocclanbot.war.service.WarLogService;
import com.chapeullah.cocclanbot.war.service.WarService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JdkHttpDiagnosticsTest {

    @Test
    void successfulResponseIsUnchangedAndBodyProgressReflectsConsumedBytes() throws Exception {
        byte[] body = "{\"items\":[],\"text\":\"Привет\"}".getBytes(StandardCharsets.UTF_8);
        try (LocalServer server = new LocalServer(exchange -> respond(exchange, 200, body));
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realSuccess", 1)) {
            diagnostics.beginAttempt(1);
            String actual = client(http, server.url(), Duration.ofSeconds(5)).get().retrieve().body(String.class);
            diagnostics.succeeded();
            assertEquals(new String(body, StandardCharsets.UTF_8), actual);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("responseHeadersReceived=true"), summary);
            assertTrue(summary.contains("status=200"), summary);
            // StringHttpMessageConverter reads exactly Content-Length without probing for EOF.
            assertTrue(summary.contains("responseBodyEndObserved=false"), summary);
            assertEquals(body.length, number(summary, "responseBytesRead"));
            assertTrue(summary.contains("stage=COMPLETE"), summary);
        }
        assertNull(HttpRequestDiagnostics.current());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void readingBodyTwiceUsesTheSameStreamAndExchangeClosesTheResponse() throws Exception {
        byte[] body = "response-bytes".getBytes(StandardCharsets.UTF_8);
        AtomicReference<java.io.InputStream> receivedBody = new AtomicReference<>();
        try (LocalServer server = new LocalServer(exchange -> respond(exchange, 200, body));
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realExchange", 1)) {
            diagnostics.beginAttempt(1);
            byte[] actual = client(http, server.url(), Duration.ofSeconds(5)).get().exchange((request, response) -> {
                var stream = response.getBody();
                receivedBody.set(stream);
                assertSame(stream, response.getBody());
                return stream.readAllBytes();
            });
            assertArrayEquals(body, actual);
            assertTrue(diagnostics.summary().contains("responseBodyEndObserved=true"));
            assertThrows(IOException.class, () -> receivedBody.get().read());
            assertEquals(body.length, number(diagnostics.summary(), "responseBytesRead"));
        }
    }

    @Test
    void httpFailurePreservesTheEntireOriginalResponseWhileTheLogSnippetIsBounded() throws Exception {
        byte[] body = ("maintenance-\u00e9-" + "x".repeat(5000)).getBytes(StandardCharsets.ISO_8859_1);
        try (LocalServer server = new LocalServer(exchange -> {
                 exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=ISO-8859-1");
                 exchange.getResponseHeaders().set("Retry-After", "10");
                 respond(exchange, 503, body);
             });
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realStatus", 1)) {
            diagnostics.beginAttempt(1);
            var failure = assertThrows(RestClientResponseException.class,
                    () -> client(http, server.url(), Duration.ofSeconds(5)).get().retrieve().body(String.class));
            diagnostics.failed(failure);
            assertEquals(503, failure.getStatusCode().value());
            assertArrayEquals(body, failure.getResponseBodyAsByteArray());
            String summary = diagnostics.summary();
            assertTrue(summary.contains("errorType=HTTP_STATUS"), summary);
            assertTrue(summary.contains("Retry-After=10"), summary);
            assertTrue(summary.contains("<truncated>"), summary);
            assertFalse(summary.contains("x".repeat(HttpRequestDiagnostics.ERROR_BODY_LIMIT + 1)), summary);
            assertEquals(body.length, number(summary, "responseBytesRead"));
        }
    }

    @Test
    void malformedJsonAfterRealHttpResponseIsADecodingFailure() throws Exception {
        try (LocalServer server = new LocalServer(exchange -> respond(exchange, 200,
                     "{invalid".getBytes(StandardCharsets.UTF_8)));
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realDecoding", 1)) {
            diagnostics.beginAttempt(1);
            var failure = assertThrows(RestClientException.class,
                    () -> client(http, server.url(), Duration.ofSeconds(5)).get().retrieve().body(ClanResponse.class));
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("status=200"), summary);
            assertTrue(summary.contains("errorType=RESPONSE_DECODING"), summary);
            assertTrue(summary.contains("HttpMessageNotReadableException"), summary);
        }
    }

    @Test
    void truncatedHttpBodyRetainsStatusAndReportsTheOriginalIoCause() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             ExecutorService server = Executors.newSingleThreadExecutor();
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realTruncatedBody", 1)) {
            listener.setSoTimeout(5000);
            var served = server.submit(() -> {
                try (Socket socket = listener.accept()) {
                    socket.setSoTimeout(5000);
                    readHeaders(socket);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"
                            + "Content-Length: 100\r\nConnection: close\r\n\r\n{\"items\":[]}")
                            .getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                }
                return null;
            });
            diagnostics.beginAttempt(1);
            var failure = assertThrows(RestClientException.class,
                    () -> client(http, "http://127.0.0.1:" + listener.getLocalPort(), Duration.ofSeconds(5))
                            .get().retrieve().body(String.class));
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("status=200"), summary);
            assertTrue(summary.contains("responseHeadersReceived=true"), summary);
            assertTrue(summary.contains("responseBodyEndObserved=false"), summary);
            assertTrue(summary.contains("errorType=RESPONSE_BODY_IO"), summary);
            assertTrue(summary.contains("IOException"), summary);
            assertTrue(number(summary, "responseBytesRead") <= 12, summary);
            served.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void connectionRefusalIsAConnectionFailure() throws Exception {
        int closedPort;
        try (ServerSocket unused = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            closedPort = unused.getLocalPort();
        }
        try (HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realConnectionFailure", 1)) {
            diagnostics.beginAttempt(1);
            var failure = assertThrows(ResourceAccessException.class,
                    () -> client(http, "http://127.0.0.1:" + closedPort, Duration.ofSeconds(5))
                            .get().retrieve().body(String.class));
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("errorType=CONNECT_FAILURE"), summary);
            assertTrue(summary.contains("ConnectException"), summary);
            assertTrue(summary.contains("responseHeadersReceived=false"), summary);
        }
    }

    @Test
    void rejectedTlsHandshakeIsReportedAsTlsFailure() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             ExecutorService server = Executors.newSingleThreadExecutor();
             HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                     .connectTimeout(Duration.ofSeconds(15)).build();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realTlsFailure", 1)) {
            listener.setSoTimeout(15000);
            var served = server.submit(() -> {
                try (Socket socket = listener.accept()) {
                    socket.setSoTimeout(15000);
                    var input = socket.getInputStream();
                    byte[] header = input.readNBytes(5);
                    assertEquals(5, header.length);
                    assertEquals(22, header[0] & 0xff);
                    int length = ((header[3] & 0xff) << 8) | (header[4] & 0xff);
                    assertEquals(length, input.readNBytes(length).length);
                    socket.getOutputStream().write(new byte[]{21, 3, 3, 0, 2, 2, 40});
                    socket.getOutputStream().flush();
                    input.transferTo(java.io.OutputStream.nullOutputStream());
                }
                return null;
            });
            diagnostics.beginAttempt(1);
            var failure = assertThrows(ResourceAccessException.class,
                    () -> client(http, "https://127.0.0.1:" + listener.getLocalPort(), Duration.ofSeconds(20))
                            .get().retrieve().body(String.class));
            served.get(15, TimeUnit.SECONDS);
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("errorType=TLS_FAILURE"), summary);
            assertTrue(summary.contains("SSLHandshakeException"), summary);
            assertTrue(summary.contains("responseHeadersReceived=false"), summary);
        }
    }

    @Test
    void timeoutBeforeHeadersIsReportedAsHttpTimeout() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (LocalServer server = new LocalServer(exchange -> awaitRelease(release));
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realHeadersTimeout", 1)) {
            diagnostics.beginAttempt(1);
            var failure = assertThrows(ResourceAccessException.class,
                    () -> client(http, server.url(), Duration.ofMillis(500)).get().retrieve().body(String.class));
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("errorType=HTTP_TIMEOUT"), summary);
            assertTrue(summary.contains("HttpTimeoutException"), summary);
            assertTrue(summary.contains("responseHeadersReceived=false"), summary);
            assertTrue(summary.contains("stage=AWAITING_RESPONSE_HEADERS"), summary);
        } finally {
            release.countDown();
        }
    }

    @Test
    void springBodyTimeoutIsReportedAsObservedIoFailureWithoutInventingAHttpTimeoutCause() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (LocalServer server = new LocalServer(exchange -> {
                 exchange.getResponseHeaders().set("Content-Type", "application/json");
                 exchange.sendResponseHeaders(200, 100);
                 exchange.getResponseBody().write('{');
                 exchange.getResponseBody().flush();
                 awaitRelease(release);
             });
             HttpClient http = httpClient();
             HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realBodyTimeout", 1)) {
            diagnostics.beginAttempt(1);
            var failure = assertThrows(RestClientException.class,
                    () -> client(http, server.url(), Duration.ofMillis(500)).get().retrieve().body(String.class));
            diagnostics.failed(failure);
            String summary = diagnostics.summary();
            assertTrue(summary.contains("responseHeadersReceived=true"), summary);
            assertTrue(summary.contains("status=200"), summary);
            assertTrue(summary.contains("errorType=RESPONSE_BODY_IO"), summary);
            assertTrue(summary.contains("responseBodyEndObserved=false"), summary);
            assertTrue(summary.contains("IOException"), summary);
            assertFalse(summary.contains("HttpTimeoutException"), summary);
            assertEquals(1, number(summary, "responseBytesRead"));
        } finally {
            release.countDown();
        }
    }

    @Test
    void interruptionBeforeHeadersPreservesTheInterruptAndClearsDiagnostics() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<String> summary = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean cleaned = new AtomicBoolean();
        try (LocalServer server = new LocalServer(exchange -> {
                 entered.countDown();
                 awaitRelease(release);
             });
             HttpClient http = httpClient()) {
            RestClient client = client(http, server.url(), Duration.ofSeconds(5));
            Thread requester = Thread.ofPlatform().start(() -> {
                try (HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics("realCancellation", 1)) {
                    diagnostics.beginAttempt(1);
                    try {
                        client.get().retrieve().body(String.class);
                    } catch (Throwable exception) {
                        diagnostics.failed(exception);
                        failure.set(exception);
                        summary.set(diagnostics.summary());
                        interrupted.set(Thread.currentThread().isInterrupted());
                    }
                } finally {
                    cleaned.set(HttpRequestDiagnostics.current() == null && MDC.get("requestId") == null);
                }
            });
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                requester.interrupt();
                requester.join(5000);
                assertFalse(requester.isAlive());
                assertInstanceOf(ResourceAccessException.class, failure.get());
                assertTrue(summary.get().contains("errorType=REQUEST_CANCELLED"), summary.get());
                assertTrue(summary.get().contains("InterruptedException"), summary.get());
                assertTrue(interrupted.get());
                assertTrue(cleaned.get());
            } finally {
                release.countDown();
                requester.interrupt();
                requester.join(5000);
            }
        } finally {
            release.countDown();
        }
    }

    @Test
    void schedulerDeadlineInterruptsRealBodyReadOnceAndPreservesItsCauseAndSnapshot() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<RuntimeException> requestFailure = new AtomicReference<>();
        ClanService clans = mock(ClanService.class);
        RaidSeasonService raids = mock(RaidSeasonService.class);
        TelegramWarNotificationService notifications = mock(TelegramWarNotificationService.class);
        Scheduler scheduler = new Scheduler(notifications, mock(WarLogService.class), mock(WarService.class), clans, raids);
        ReflectionTestUtils.setField(scheduler, "clanTag", "#TEST");
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 1000L);
        Logger schedulerLogger = (Logger) LoggerFactory.getLogger(Scheduler.class);
        Logger clientLogger = (Logger) LoggerFactory.getLogger(ClashOfClansClient.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        events.start();
        schedulerLogger.addAppender(events);
        clientLogger.addAppender(events);
        try (LocalServer server = new LocalServer(exchange -> {
                 if (exchange.getRequestURI().getPath().equals("/warmup")) {
                     respond(exchange, 200, "{}".getBytes(StandardCharsets.UTF_8));
                     return;
                 }
                 requests.incrementAndGet();
                 exchange.getResponseHeaders().set("Content-Type", "application/json");
                 exchange.sendResponseHeaders(200, 100);
                 exchange.getResponseBody().write('{');
                 exchange.getResponseBody().flush();
                 awaitRelease(release);
             });
             HttpClient http = httpClient()) {
            RestClient rest = client(http, server.url(), Duration.ofSeconds(5));
            assertEquals("{}", rest.get().uri("/warmup").retrieve().body(String.class));
            ClashOfClansClient clash = new ClashOfClansClient(rest);
            ReflectionTestUtils.setField(clash, "apiToken", "fake-local-body-cancellation-token");
            when(clans.syncClan("#TEST")).thenAnswer(invocation -> {
                try {
                    clash.getCurrentRaid("#TEST");
                } catch (RuntimeException failure) {
                    requestFailure.set(failure);
                    throw failure;
                }
                return null;
            });
            assertDoesNotThrow(scheduler::synchronizeClanAndRaids);
            assertEquals(1, requests.get());
            assertNotNull(requestFailure.get());
            assertTrue(HttpRequestDiagnostics.causeChain(requestFailure.get()).contains("InterruptedException"));
            var errors = events.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
            assertEquals(1, errors.size());
            String message = errors.getFirst().getFormattedMessage();
            assertTrue(message.contains("cancellationReason=SYNC_DEADLINE"), message);
            assertTrue(message.contains("InterruptedException"), message);
            assertNotNull(errors.getFirst().getThrowableProxy());
            assertTrue(message.contains("atDeadline={"), message);
            String snapshot = message.substring(message.indexOf("atDeadline={"));
            assertTrue(snapshot.contains("stage=READING_RESPONSE_BODY"), snapshot);
            assertTrue(snapshot.contains("status=200"), snapshot);
            assertTrue(snapshot.contains("responseBytesRead=1"), snapshot);
            var httpFailures = events.list.stream().filter(event -> event.getFormattedMessage().startsWith("HTTP request failed")).toList();
            assertEquals(1, httpFailures.size());
            assertTrue(httpFailures.getFirst().getFormattedMessage().contains("retrying=false"));
            assertTrue(httpFailures.getFirst().getFormattedMessage().contains("errorType=SYNC_DEADLINE"));
            assertEquals(errors.getFirst().getMDCPropertyMap().get("syncId"),
                    httpFailures.getFirst().getMDCPropertyMap().get("syncId"));
            assertNotNull(httpFailures.getFirst().getMDCPropertyMap().get("requestId"));
            ArgumentCaptor<Exception> notification = ArgumentCaptor.forClass(Exception.class);
            verify(notifications).sendError(eq("Clan and Raid synchronization"), notification.capture());
            assertSame(requestFailure.get(), notification.getValue().getCause());
            verifyNoMoreInteractions(notifications);
            verifyNoInteractions(raids);
            assertFalse(Thread.currentThread().isInterrupted());
            assertNull(HttpRequestDiagnostics.current());
            assertNull(SynchronizationDiagnostics.current());
            assertNull(MDC.get("requestId"));
            assertNull(MDC.get("syncId"));
        } finally {
            release.countDown();
            scheduler.shutdown();
            schedulerLogger.detachAppender(events);
            clientLogger.detachAppender(events);
            Thread.interrupted();
            MDC.clear();
        }
    }

    private static HttpClient httpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2)).build();
    }

    private static RestClient client(HttpClient http, String url, Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        return HttpConfig.withDiagnostics(RestClient.builder().requestFactory(factory).baseUrl(url)).build();
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        }
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static void awaitRelease(CountDownLatch release) throws IOException {
        try {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IOException("test server release timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void readHeaders(Socket socket) throws IOException {
        int matched = 0;
        byte[] ending = {'\r', '\n', '\r', '\n'};
        while (matched < ending.length) {
            int value = socket.getInputStream().read();
            if (value < 0) throw new IOException("request ended before headers");
            matched = value == ending[matched] ? matched + 1 : value == ending[0] ? 1 : 0;
        }
    }

    private static long number(String message, String field) {
        var matcher = Pattern.compile("\\b" + field + "=(\\d+)").matcher(message);
        assertTrue(matcher.find(), message);
        return Long.parseLong(matcher.group(1));
    }

    private static final class LocalServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        private LocalServer(HttpHandler handler) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
            server.createContext("/", exchange -> {
                try (exchange) {
                    handler.handle(exchange);
                }
            });
            server.start();
        }

        private String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() throws InterruptedException {
            server.stop(0);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
