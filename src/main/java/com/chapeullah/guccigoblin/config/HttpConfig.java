package com.chapeullah.guccigoblin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@DependsOn("environmentVariablesValidator")
public class HttpConfig {

    @Value("${coc.base-url}")
    private String baseUrl;

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("HTTP request: method=\"{}\", uri=\"{}\"", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(status -> status.value() != 200,
                        (request, response) -> {
                            throw new RestClientResponseException(
                                    "Unexpected HTTP status: "
                                            + response.getStatusCode()
                                            + " for " + request.getURI(),
                                    response.getStatusCode(),
                                    response.getStatusText(),
                                    response.getHeaders(),
                                    response.getBody().readAllBytes(),
                                    null);
                        })
                .build();
    }
}
