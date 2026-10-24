package com.chapeullah.cocclanbot.diagnostics;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownContentTypeException;
import javax.net.ssl.SSLException;
import java.io.FilterInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HttpRequestDiagnostics implements AutoCloseable {
    public static final int CONNECT_TIMEOUT_MS = 5000;
    public static final int READ_TIMEOUT_MS = 15000;
    public static final int ERROR_BODY_LIMIT = 2048;
    private static final ThreadLocal<HttpRequestDiagnostics> CURRENT = new ThreadLocal<>();
    private final HttpRequestDiagnostics previous;
    private final String previousRequestId;
    private final String requestId = UUID.randomUUID().toString();
    private final String operation;
    private final int maxAttempts;
    private final SynchronizationDiagnostics synchronization = SynchronizationDiagnostics.current();
    private final long startedAt = System.nanoTime();
    private volatile long endedAt;
    private volatile long attemptStartedAt;
    private volatile long attemptEndedAt;
    private volatile int attempt;
    private volatile String method = "GET";
    private volatile String uri = "unknown";
    private volatile String stage = "PREPARING_REQUEST";
    private volatile boolean headersReceived;
    private volatile boolean bodyEndObserved;
    private volatile boolean bodyReadFailed;
    private volatile long headersMs = -1;
    private volatile long bytesRead;
    private volatile Integer status;
    private volatile String responseDetails = "none";
    private volatile String errorBody = "none";
    private volatile Throwable failure;
    private boolean cancelledByDeadline;
    private final ByteArrayOutputStream errorResponsePrefix = new ByteArrayOutputStream();

    public HttpRequestDiagnostics(String operation, int maxAttempts) {
        this.operation = operation;
        this.maxAttempts = maxAttempts;
        previous = CURRENT.get();
        previousRequestId = MDC.get("requestId");
        CURRENT.set(this);
        MDC.put("requestId", requestId);
        if (SynchronizationDiagnostics.current() != null) {
            SynchronizationDiagnostics.current().request(this);
        }
    }

    public static HttpRequestDiagnostics current() {
        return CURRENT.get();
    }

    public synchronized void beginAttempt(int attempt) {
        this.attempt = attempt;
        attemptStartedAt = System.nanoTime();
        attemptEndedAt = 0;
        stage = "PREPARING_REQUEST";
        headersReceived = false;
        bodyEndObserved = false;
        bodyReadFailed = false;
        headersMs = -1;
        bytesRead = 0;
        status = null;
        responseDetails = "none";
        errorBody = "none";
        failure = null;
        cancelledByDeadline = false;
        errorResponsePrefix.reset();
    }

    public synchronized void succeeded() {
        attemptEndedAt = System.nanoTime();
        endedAt = attemptEndedAt;
        stage = "COMPLETE";
    }

    public synchronized void failed(Throwable exception) {
        if (attemptEndedAt == 0) {
            attemptEndedAt = System.nanoTime();
        }
        failure = exception;
        cancelledByDeadline = synchronization != null && synchronization.isDeadlineExceeded()
                && (Thread.currentThread().isInterrupted() || isCancellation(exception));
        if ("RESPONSE_DECODING".equals(errorType(exception, bodyReadFailed, cancelledByDeadline))) {
            stage = "DECODING_RESPONSE";
        }
    }

    public synchronized void finish() {
        if (endedAt == 0) endedAt = System.nanoTime();
    }

    public synchronized void retryDelay() {
        stage = "RETRY_DELAY";
    }

    public synchronized void errorBody(byte[] bytes) {
        errorBody = DiagnosticSanitizer.bodySnippet(bytes, ERROR_BODY_LIMIT);
    }

    public synchronized String summary() {
        long now = System.nanoTime();
        SynchronizationDiagnostics sync = synchronization;
        long duration = attemptStartedAt == 0 ? 0
                : TimeUnit.NANOSECONDS.toMillis((attemptEndedAt == 0 ? now : attemptEndedAt) - attemptStartedAt);
        return "requestId=" + requestId + ", operation=" + operation + ", method=" + method
                + ", uri=\"" + uri + "\", attempt=" + attempt + "/" + maxAttempts + ", durationMs=" + duration
                + ", totalDurationMs=" + TimeUnit.NANOSECONDS.toMillis((endedAt == 0 ? now : endedAt) - startedAt)
                + ", connectTimeoutMs=" + CONNECT_TIMEOUT_MS + ", readTimeoutMs=" + READ_TIMEOUT_MS
                + ", syncRemainingMs=" + (sync == null ? -1 : sync.remainingMs())
                + ", stage=" + stage + ", responseHeadersReceived=" + headersReceived
                + ", headersDurationMs=" + headersMs + ", status=" + (status == null ? "none" : status)
                + ", responseBytesRead=" + bytesRead + ", responseBodyEndObserved=" + bodyEndObserved
                + ", errorType=" + errorType(failure, bodyReadFailed, cancelledByDeadline)
                + ", cancellationReason=" + (cancelledByDeadline ? "SYNC_DEADLINE" : "none")
                + ", causeChain=\"" + causeChain(failure) + "\", causeMessage=\"" + causeMessage(failure)
                + "\", responseDetails=\"" + responseDetails + "\", errorBody=\""
                + (errorResponsePrefix.size() == 0 ? errorBody
                : DiagnosticSanitizer.bodySnippet(errorResponsePrefix.toByteArray(), ERROR_BODY_LIMIT)) + "\"";
    }

    public static ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                               ClientHttpRequestExecution execution) throws IOException {
        HttpRequestDiagnostics diagnostics = current();
        if (diagnostics == null) {
            return execution.execute(request, body);
        }
        synchronized (diagnostics) {
            diagnostics.method = request.getMethod().name();
            diagnostics.uri = DiagnosticSanitizer.uri(request.getURI());
            diagnostics.stage = "AWAITING_RESPONSE_HEADERS";
        }
        ClientHttpResponse response = execution.execute(request, body);
        try {
            synchronized (diagnostics) {
                diagnostics.headersReceived = true;
                diagnostics.headersMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - diagnostics.attemptStartedAt);
                diagnostics.stage = "RESPONSE_HEADERS_RECEIVED";
                diagnostics.status = response.getStatusCode().value();
                diagnostics.responseDetails = selectedHeaders(response.getHeaders());
            }
        } catch (IOException | RuntimeException | Error failure) {
            try {
                response.close();
            } catch (RuntimeException | Error closeFailure) {
                if ((Throwable) closeFailure != failure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
        return new ClientHttpResponse() {
            private InputStream stream;

            @Override
            public HttpStatusCode getStatusCode() throws IOException {
                return response.getStatusCode();
            }

            @Override
            public String getStatusText() throws IOException {
                return response.getStatusText();
            }

            @Override
            public HttpHeaders getHeaders() {
                return response.getHeaders();
            }

            @Override
            public InputStream getBody() throws IOException {
                if (stream == null) {
                    InputStream bodyStream;
                    try {
                        bodyStream = response.getBody();
                    } catch (IOException exception) {
                        diagnostics.bodyReadFailed = true;
                        diagnostics.stage = "READING_RESPONSE_BODY";
                        throw exception;
                    }
                    stream = new FilterInputStream(bodyStream) {
                        @Override
                        public int read() throws IOException {
                            return observedRead(null, 0, 0);
                        }

                        @Override
                        public int read(byte[] bytes, int offset, int length) throws IOException {
                            return observedRead(bytes, offset, length);
                        }

                        private int observedRead(byte[] bytes, int offset, int length) throws IOException {
                            diagnostics.stage = "READING_RESPONSE_BODY";
                            try {
                                int result = bytes == null ? in.read() : in.read(bytes, offset, length);
                                synchronized (diagnostics) {
                                    if (result < 0) {
                                        diagnostics.bodyEndObserved = true;
                                        diagnostics.stage = "PROCESSING_RESPONSE";
                                    } else {
                                        diagnostics.bytesRead += bytes == null ? 1 : result;
                                        if (diagnostics.status != null && diagnostics.status != 200) {
                                            int remaining = ERROR_BODY_LIMIT + 1 - diagnostics.errorResponsePrefix.size();
                                            if (remaining > 0) {
                                                if (bytes == null) diagnostics.errorResponsePrefix.write(result);
                                                else diagnostics.errorResponsePrefix.write(bytes, offset, Math.min(remaining, result));
                                            }
                                        }
                                    }
                                }
                                return result;
                            } catch (IOException exception) {
                                diagnostics.bodyReadFailed = true;
                                throw exception;
                            }
                        }
                    };
                }
                return stream;
            }

            @Override
            public void close() {
                String previousStage = diagnostics.stage;
                diagnostics.stage = "CLOSING_RESPONSE";
                try {
                    response.close();
                } finally {
                    diagnostics.stage = previousStage;
                }
            }
        };
    }

    private static String selectedHeaders(HttpHeaders headers) {
        return Stream.of("Content-Type",
                        "Date",
                        "Retry-After",
                        "X-Request-ID",
                        "X-Correlation-ID",
                        "CF-Ray")
                .filter(name -> headers.getFirst(name) != null)
                .map(name -> name + "=" + DiagnosticSanitizer.singleLine(headers.getFirst(name), 160))
                .collect(Collectors.joining("; "));
    }

    private static List<Throwable> causes(Throwable exception) {
        List<Throwable> result = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (exception != null && result.size() < 24 && visited.add(exception)) {
            result.add(exception);
            exception = exception.getCause();
        }
        return result;
    }

    public static String causeChain(Throwable exception) {
        List<Throwable> chain = causes(exception);
        return chain.isEmpty() ? "none" : chain.stream()
                .map(cause -> cause.getClass().getSimpleName()).collect(Collectors.joining(" -> "));
    }

    private static String causeMessage(Throwable exception) {
        List<Throwable> chain = causes(exception);
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (chain.get(i).getMessage() != null && !chain.get(i).getMessage().isBlank()) {
                return DiagnosticSanitizer.singleLine(chain.get(i).getMessage(), 400);
            }
        }
        return "none";
    }

    public static String errorType(Throwable exception, boolean bodyReadFailed, boolean deadlineExceeded) {
        if (exception == null) return "none";
        List<Throwable> chain = causes(exception);
        if (chain.stream().anyMatch(e -> e instanceof UnknownHostException || e instanceof UnresolvedAddressException)) return "DNS_FAILURE";
        if (chain.stream().anyMatch(HttpConnectTimeoutException.class::isInstance)) return "CONNECT_TIMEOUT";
        if (chain.stream().anyMatch(SSLException.class::isInstance)) return "TLS_FAILURE";
        if (chain.stream().anyMatch(e -> e instanceof HttpTimeoutException || e instanceof SocketTimeoutException)) return "HTTP_TIMEOUT";
        if (isCancellation(exception)) return deadlineExceeded ? "SYNC_DEADLINE" : "REQUEST_CANCELLED";
        if (bodyReadFailed) return "RESPONSE_BODY_IO";
        if (chain.stream().anyMatch(RestClientResponseException.class::isInstance)) return "HTTP_STATUS";
        if (chain.stream().anyMatch(ConnectException.class::isInstance)) return "CONNECT_FAILURE";
        if (chain.stream().anyMatch(SocketException.class::isInstance)) return "CONNECTION_IO";
        if (chain.stream().anyMatch(e -> e instanceof HttpMessageConversionException
                || e instanceof UnknownContentTypeException
                || e.getClass().getName().startsWith("tools.jackson."))) return "RESPONSE_DECODING";
        if (chain.stream().anyMatch(IOException.class::isInstance)) return "IO_FAILURE";
        return "UNEXPECTED_FAILURE";
    }

    private static boolean isCancellation(Throwable exception) {
        return causes(exception).stream()
                .anyMatch(e -> e instanceof InterruptedException || e instanceof CancellationException);
    }

    @Override
    public void close() {
        finish();
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        if (previousRequestId == null) MDC.remove("requestId"); else MDC.put("requestId", previousRequestId);
    }
}
