package com.chapeullah.cocclanbot.config;

import com.chapeullah.cocclanbot.diagnostics.DiagnosticSanitizer;
import com.chapeullah.cocclanbot.diagnostics.HttpRequestDiagnostics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@DependsOn("environmentVariablesService")
public class HttpConfig {

    @Value("${coc.base-url}")
    private String baseUrl;

    @Value("${coc.api-token}")
    private String apiToken;

    @Value("${TELEGRAM_BOT_TOKEN:}")
    private String telegramToken;

    @Bean
    public RestClient restClient() {
        DiagnosticSanitizer.registerSecret(apiToken);
        DiagnosticSanitizer.registerSecret(telegramToken);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(HttpRequestDiagnostics.CONNECT_TIMEOUT_MS))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(HttpRequestDiagnostics.READ_TIMEOUT_MS));

        return withDiagnostics(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)).build();
    }

    public static RestClient.Builder withDiagnostics(RestClient.Builder builder) {
        return builder
                .requestInterceptor(HttpRequestDiagnostics::intercept)
                .defaultStatusHandler(status -> status.value() != 200,
                        (request, response) -> {
                            byte[] errorBody = response.getBody().readAllBytes();
                            if (HttpRequestDiagnostics.current() != null) {
                                HttpRequestDiagnostics.current().errorBody(errorBody);
                            }
                            throw new RestClientResponseException(
                                    "Unexpected HTTP status: "
                                            + response.getStatusCode() + " for " + DiagnosticSanitizer.uri(request.getURI()),
                                    response.getStatusCode(),
                                    response.getStatusText(),
                                    response.getHeaders(),
                                    errorBody,
                                    null);
                        });
    }
}
