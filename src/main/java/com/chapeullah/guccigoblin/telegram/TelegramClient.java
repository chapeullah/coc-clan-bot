package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.dto.TelegramApiResponse;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("environmentVariablesValidator")
public class TelegramClient {

    private final RestClient restClient;

    public TelegramClient(@Value("${telegram.bot.token}") String token) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(35));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(requestFactory)
                .build();
    }

    public List<TelegramUpdate> getUpdates(long offset) {
        TelegramApiResponse<List<TelegramUpdate>> response = restClient.get()
                .uri(builder -> builder
                        .path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", 25)
                        .queryParam("allowed_updates", "[\"message\"]")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || !response.ok()) {
            throw new IllegalStateException("Telegram did not return updates");
        }

        return response.result() == null
                ? List.of()
                : response.result();
    }

    public void sendMessage(long chatId, String text) {
        restClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text,
                        "parse_mode", "HTML"))
                .retrieve()
                .toBodilessEntity();
    }

}