package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.dto.TelegramApiResponse;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("environmentVariablesValidator")
public class TelegramClient {

    private static final int MESSAGE_LENGTH_LIMIT = 4000;

    private final RestClient restClient;

    private String username;

    public TelegramClient(@Value("${TELEGRAM_BOT_TOKEN}") String token) {
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

    public synchronized String getUsername() {
        if (username != null) {
            return username;
        }

        TelegramApiResponse<TelegramUser> response = restClient.get()
                .uri("/getMe")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || !response.ok() || response.result() == null
                || response.result().username() == null || response.result().username().isBlank()) {
            throw new IllegalStateException("Telegram did not return the bot username");
        }

        username = response.result().username();
        return username;
    }

    public void sendMessage(long chatId, String text) {
        sendMessage(chatId, null, text);
    }

    public void sendMessage(long chatId, Integer threadId, String text) {
        for (String part : splitMessage(text)) {
            sendMessagePart(chatId, threadId, part);
        }
    }

    private void sendMessagePart(long chatId, Integer threadId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        if (threadId != null) {
            body.put("message_thread_id", threadId);
        }
        restClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private List<String> splitMessage(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Telegram message must not be null");
        }

        List<String> parts = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > MESSAGE_LENGTH_LIMIT) {
            int splitIndex = remaining.lastIndexOf("\n\n", MESSAGE_LENGTH_LIMIT);
            if (splitIndex <= 0) {
                splitIndex = remaining.lastIndexOf('\n', MESSAGE_LENGTH_LIMIT);
            }
            if (splitIndex <= 0) {
                throw new IllegalArgumentException(
                        "Telegram message contains a line longer than %d characters"
                                .formatted(MESSAGE_LENGTH_LIMIT));
            }

            parts.add(remaining.substring(0, splitIndex));
            remaining = remaining.substring(splitIndex).stripLeading();
        }
        if (!remaining.isBlank()) {
            parts.add(remaining);
        }
        return parts;
    }

}
