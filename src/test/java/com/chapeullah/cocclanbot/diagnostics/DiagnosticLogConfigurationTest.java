package com.chapeullah.cocclanbot.diagnostics;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.slf4j.MDC;
import org.springframework.web.client.ResourceAccessException;
import java.io.ByteArrayInputStream;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.sun.net.httpserver.HttpServer;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticLogConfigurationTest {

    @Test
    void actualLogConfigurationIncludesCorrelationAndMasksSecretsInMessagesAndStackTraces() throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "diagnostic-log-");
        String secret = "configuration-test-private-api-token";
        DiagnosticSanitizer.registerSecret(secret);
        String xml;
        try (var stream = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertNotNull(stream);
            xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        xml = xml.replaceAll("<springProperty[^>]+/>", "")
                .replace("<configuration>", "<configuration><property name=\"LOG_PATH\" value=\""
                        + directory.toString().replace('\\', '/') + "\"/>");
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(MDC.getMDCAdapter());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            assertTrue(context.getStatusManager().getCopyOfStatusList().stream()
                    .noneMatch(status -> status.getLevel() == Status.ERROR));
            MDC.put("syncId", "configuration-sync");
            MDC.put("requestId", "configuration-request");
            var failure = new IllegalStateException("https://api.telegram.org/bot123:private/sendMessage "
                    + secret.substring(0, 25) + "\r\nforged-line\u001b[31m");
            failure.addSuppressed(new IllegalArgumentException("Authorization: Basic hidden-credentials\u2028forged"));
            context.getLogger("jdk.httpclient.HttpClient").warn("Bearer " + secret + "\nforged-event", failure);
            context.getLogger("short-message").warn("\n", new IllegalStateException("\n"));
            context.getLogger("diagnostic-fields").warn(
                    "causeMessage=\"{}\", responseDetails=\"{}\", errorBody=\"{}\"",
                    DiagnosticSanitizer.singleLine("Authorization: Basic details-private", 400),
                    "Content-Type=application/json", "original-error-body");
            context.getLogger("literal-marker").warn("Authorization: <redacted> Basic fake-marker-private");
        } finally {
            context.stop();
            MDC.clear();
        }
        String log = Files.readString(directory.resolve("coc-clan-bot.log"));
        assertTrue(log.contains("syncId=configuration-sync"));
        assertTrue(log.contains("requestId=configuration-request"));
        assertTrue(log.contains("<redacted>"));
        assertFalse(log.contains(secret));
        assertFalse(log.contains("123:private"));
        assertFalse(log.contains(secret.substring(0, 25)));
        assertFalse(log.contains("hidden-credentials"));
        assertFalse(log.contains("details-private"));
        assertFalse(log.contains("fake-marker-private"));
        assertFalse(log.contains("\nforged"));
        assertTrue(log.contains("\\u000aforged-event"));
        assertFalse(log.contains("\u001b"));
        assertFalse(log.contains("\u2028"));
        assertTrue(log.contains("Suppressed: java.lang.IllegalArgumentException"));
        assertTrue(log.contains("\tat "), "Stack frames must remain readable");
        assertTrue(log.contains("java.lang.IllegalStateException: \\u000a"));
        assertTrue(log.contains("causeMessage=\"Authorization: <redacted>\", "
                + "responseDetails=\"Content-Type=application/json\", errorBody=\"original-error-body\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "errors,ssl,channel", "errors,requests,headers,channel"})
    void realJdkTransportLoggingPassesThroughBootAndTheMaskingLayouts(String options) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "transport-log-");
        Path console = directory.resolve("console.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djdk.httpclient.HttpClient.log=" + options,
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"),
                "-Djdk.net.unixdomain.tmpdir=" + System.getProperty("jdk.net.unixdomain.tmpdir", System.getProperty("java.io.tmpdir")),
                "-cp", System.getProperty("java.class.path"), TransportProbe.class.getName(), directory.toString())
                .redirectErrorStream(true).redirectOutput(console.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Transport probe timed out");
            assertEquals(0, process.exitValue(), () -> read(console));
            for (Path output : new Path[]{console, directory.resolve("coc-clan-bot.log")}) {
                String log = Files.readString(output);
                assertEquals(options.contains("requests"), log.contains("REQUEST:"), log);
                assertEquals(options.contains("headers"), log.contains("HEADERS:"), log);
                assertEquals(options.contains("channel"), log.contains("CHANNEL:"), log);
                if (options.contains("headers")) {
                    assertTrue(log.contains("<redacted>"), log);
                }
                assertFalse(log.contains(TransportProbe.TOKEN), log);
                assertFalse(log.contains(TransportProbe.BASIC_CREDENTIALS), log);
                assertFalse(log.contains(TransportProbe.TOKEN.substring(0, 16)), log);
            }
        } finally {
            process.destroyForcibly();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            return exception.toString();
        }
    }

    public static class TransportProbe {
        static final String TOKEN = "345678:fictional-transport-token";
        static final String BASIC_CREDENTIALS = "ZmFrZTpmYWtl";

        public static void main(String[] args) throws Exception {
            DiagnosticSanitizer.registerSecret(TOKEN);
            var environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("LOG_DIR", args[0], "spring.output.ansi.enabled", "never")));
            LoggingSystem logging = LoggingSystem.get(TransportProbe.class.getClassLoader());
            logging.beforeInitialize();
            logging.initialize(new LoggingInitializationContext(environment), "classpath:logback-spring.xml", null);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getResponseHeaders().add("X-Request-ID", TOKEN.substring(0, 16));
                exchange.sendResponseHeaders(200, 2);
                try (var output = exchange.getResponseBody()) {
                    output.write("{}".getBytes(StandardCharsets.UTF_8));
                }
            });
            server.start();
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                                + "/bot" + TOKEN + "/sendMessage"))
                        .timeout(Duration.ofSeconds(5)).header("Authorization", "Basic " + BASIC_CREDENTIALS).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 || !"{}".equals(response.body())) {
                    throw new AssertionError("Unexpected HTTP result");
                }
            } finally {
                server.stop(0);
                logging.cleanUp();
                ((LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory()).stop();
            }
        }
    }

    @Test
    void deadlineSnapshotKeepsRequestContextAcrossThreadsAndMdcIsRestored() throws Exception {
        MDC.put("syncId", "outer-sync");
        MDC.put("requestId", "outer-request");
        try (SynchronizationDiagnostics sync = new SynchronizationDiagnostics("sync", 55000)) {
            try (HttpRequestDiagnostics request = new HttpRequestDiagnostics("getClan", 3)) {
                request.beginAttempt(1);
                request.failed(new ResourceAccessException("timeout", new HttpTimeoutException(null)));
                Thread watchdog = Thread.ofPlatform().start(sync::deadlineExceeded);
                watchdog.join();
                assertFalse(sync.deadlineSummary().contains("syncRemainingMs=-1"));
                assertTrue(sync.deadlineSummary().contains("operation=getClan"));
                assertTrue(request.summary().contains("errorType=HTTP_TIMEOUT"));
            }
            assertEquals("outer-request", MDC.get("requestId"));
        } finally {
            assertEquals("outer-sync", MDC.get("syncId"));
            MDC.clear();
        }
    }
}
