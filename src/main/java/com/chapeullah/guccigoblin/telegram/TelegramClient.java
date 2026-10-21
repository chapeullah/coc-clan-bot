package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramApiResponse;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageService;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@DependsOn("environmentVariablesService")
public class TelegramClient {

    private static final int MESSAGE_LENGTH_LIMIT = 4000;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final TelegramOutgoingMessageService telegramOutgoingMessageService;

    private String username;
    private volatile long retryAllowedAtNanos;

    @Autowired
    public TelegramClient(@Value("${TELEGRAM_BOT_TOKEN}") String token, JsonMapper jsonMapper,
                          TelegramOutgoingMessageService telegramOutgoingMessageService) {
        this(createRestClient(token), jsonMapper, telegramOutgoingMessageService);
    }

    TelegramClient(RestClient restClient, JsonMapper jsonMapper,
                   TelegramOutgoingMessageService telegramOutgoingMessageService) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.telegramOutgoingMessageService = telegramOutgoingMessageService;
    }

    private static RestClient createRestClient(String token) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(35));

        return RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(requestFactory)
                .build();
    }

    public List<TelegramReceivedUpdate> getUpdates(long offset) {
        String rawResponse = executeRequest(() -> restClient.get()
                .uri(builder -> builder
                        .path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", 25)
                        .queryParam("allowed_updates", "[\"message\"]")
                        .build())
                .retrieve()
                .body(String.class));
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Telegram did not return updates");
        }
        TelegramApiResponse<List<JsonNode>> response = jsonMapper.readValue(
                rawResponse, new TypeReference<>() {});
        if (response == null || !response.ok()) {
            throw new IllegalStateException("Telegram did not return updates");
        }

        if (response.result() == null) {
            return List.of();
        }

        List<TelegramReceivedUpdate> updates = new ArrayList<>();
        for (JsonNode updateJson : response.result()) {
            String rawJson = updateJson.toString();
            TelegramUpdate update = jsonMapper.readValue(rawJson, TelegramUpdate.class);
            updates.add(new TelegramReceivedUpdate(update, rawJson));
        }
        return updates;
    }

    public synchronized String getUsername() {
        if (username != null) {
            return username;
        }

        TelegramApiResponse<TelegramUser> response = executeRequest(() -> restClient.get()
                .uri("/getMe")
                .retrieve()
                .body(new ParameterizedTypeReference<TelegramApiResponse<TelegramUser>>() {}));
        if (response == null || !response.ok() || response.result() == null
                || response.result().username() == null || response.result().username().isBlank()) {
            throw new IllegalStateException("Telegram did not return the bot username");
        }

        username = response.result().username();
        return username;
    }

    public void configureCommands(List<BotCommand> commands) {
        List<Map<String, Object>> allCommands = commands.stream()
                .map(command -> Map.<String, Object>of(
                        "command", command.getName().substring(1),
                        "description", command.getDescription(),
                        "is_ephemeral", true))
                .toList();
        List<Map<String, Object>> privateCommands = allCommands.stream()
                .filter(command -> TelegramAccessService.isPublicCommand("/" + command.get("command")))
                .toList();
        setCommands(allCommands, "default");
        setCommands(allCommands, "all_group_chats");
        setCommands(privateCommands, "all_private_chats");
    }

    private void setCommands(List<Map<String, Object>> commands, String scope) {
        String requestJson = jsonMapper.writeValueAsString(Map.of(
                "commands", commands,
                "scope", Map.of("type", scope)));
        TelegramApiResponse<Boolean> response = executeRequest(() -> restClient.post()
                .uri("/setMyCommands")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(new ParameterizedTypeReference<TelegramApiResponse<Boolean>>() {}));
        if (response == null || !response.ok() || !Boolean.TRUE.equals(response.result())) {
            throw new IllegalStateException("Telegram did not configure bot commands: " + scope);
        }
    }

    public void sendMessage(long chatId, String text) {
        sendMessage(chatId, null, text);
    }

    public void sendMessage(long chatId, Integer threadId, String text) {
        sendMessage(chatId, threadId, text, TelegramOutgoingMessageContext.message());
    }

    public void sendMessage(long chatId, Integer threadId, String text, TelegramOutgoingMessageContext context) {
        List<String> parts = splitMessage(text);
        for (int index = 0; index < parts.size(); index++) {
            sendMessagePart(chatId, threadId, null, null, parts.get(index), context.part(index + 1, parts.size()));
        }
    }

    void sendReplyPart(TelegramMessage message, String text, TelegramOutgoingMessageContext context) {
        long chatId = message.telegramChat().id();
        Long receiverUserId = chatId < 0 ? message.telegramUser().id() : null;
        sendMessagePart(chatId, message.threadId(), receiverUserId,
                receiverUserId == null ? null : message.ephemeralMessageId(), text, context);
    }

    private void sendMessagePart(long chatId, Integer threadId, Long receiverUserId, Integer ephemeralMessageId,
                                 String text, TelegramOutgoingMessageContext context) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        if (threadId != null) {
            body.put("message_thread_id", threadId);
        }
        if (receiverUserId != null) {
            body.put("ephemeral_message_parameters", Map.of("receiver_user_id", receiverUserId));
            if (ephemeralMessageId != null) {
                body.put("reply_parameters", Map.of("ephemeral_message_id", ephemeralMessageId));
            }
        }
        String requestJson = jsonMapper.writeValueAsString(body);
        executeRequest(() -> {
            sendAndRecord(context, chatId, threadId, receiverUserId != null, text, requestJson);
            return null;
        });
    }

    private void sendAndRecord(TelegramOutgoingMessageContext context, long chatId, Integer threadId,
                               boolean ephemeral, String text, String requestJson) {
        Long attemptId = telegramOutgoingMessageService.startAttempt(context, chatId, threadId,
                text, "HTML", ephemeral, null, requestJson);
        ResponseEntity<String> response = null;
        try {
            response = restClient.post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .toEntity(String.class);
            TelegramMessage sentMessage = readSentMessage(response);
            try {
                telegramOutgoingMessageService.markSent(attemptId, sentMessage, response.getStatusCode().value(),
                        response.getBody(), serializeHeaders(response.getHeaders()));
            } catch (RuntimeException exception) {
                // Delivery already succeeded. A history failure must not trigger a duplicate send.
                log.error("Failed to record a sent Telegram message: attemptId={}", attemptId, exception);
            }
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            TelegramOutgoingMessageStatus outcome = response == null && (status >= 500 || status == 408)
                    ? TelegramOutgoingMessageStatus.UNKNOWN : TelegramOutgoingMessageStatus.FAILED;
            recordFailure(attemptId, outcome, response == null ? status : response.getStatusCode().value(),
                    exception.getResponseBodyAsString(), serializeHeaders(exception.getResponseHeaders()), exception);
            throw exception;
        } catch (RuntimeException exception) {
            recordFailure(attemptId, TelegramOutgoingMessageStatus.UNKNOWN,
                    response == null ? null : response.getStatusCode().value(),
                    response == null ? null : response.getBody(),
                    response == null ? null : serializeHeaders(response.getHeaders()), exception);
            throw exception;
        }
    }

    private TelegramMessage readSentMessage(ResponseEntity<String> response) {
        String rawJson = response.getBody();
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Telegram returned an empty sendMessage response");
        }
        JsonNode body = jsonMapper.readTree(rawJson);
        if (body == null || !body.path("ok").isBoolean()) {
            throw new IllegalStateException("Telegram returned an invalid sendMessage response");
        }
        if (!body.path("ok").asBoolean()) {
            int status = body.path("error_code").asInt(502);
            if (status < 400 || status > 599) {
                status = 502;
            }
            throw new RestClientResponseException(body.path("description").asText("Telegram rejected the message"),
                    status, "Telegram API error", response.getHeaders(), rawJson.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        }
        if (!body.path("result").isObject()) {
            throw new IllegalStateException("Telegram did not return the sent message");
        }
        TelegramMessage message = jsonMapper.readValue(body.path("result").toString(), TelegramMessage.class);
        if (message.telegramChat() == null || message.date() <= 0) {
            throw new IllegalStateException("Telegram returned an incomplete sent message");
        }
        return message;
    }

    private void recordFailure(Long attemptId, TelegramOutgoingMessageStatus status, Integer httpStatus,
                               String rawJson, String headersJson, RuntimeException failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = TelegramUtils.maskBotToken(cause.getMessage());
        try {
            telegramOutgoingMessageService.markFailed(attemptId, status, httpStatus, rawJson,
                    headersJson, cause.getClass().getName(), message);
        } catch (RuntimeException exception) {
            log.error("Failed to record a Telegram sending failure: attemptId={}", attemptId, exception);
        }
    }

    private String serializeHeaders(HttpHeaders headers) {
        try {
            if (headers == null) {
                return null;
            }
            Map<String, List<String>> values = new HashMap<>();
            headers.forEach(values::put);
            return jsonMapper.writeValueAsString(values);
        } catch (RuntimeException exception) {
            log.warn("Failed to serialize Telegram response headers", exception);
            return null;
        }
    }

    public boolean isRateLimited() {
        return retryAllowedAtNanos != 0 && retryAllowedAtNanos - System.nanoTime() > 0;
    }

    private <T> T executeRequest(Supplier<T> request) {
        if (isRateLimited()) {
            throw new IllegalStateException("Telegram requests are paused after flood control");
        }
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                postponeRequests(exception);
            }
            throw exception;
        }
    }

    private synchronized void postponeRequests(RestClientResponseException exception) {
        int retryAfterSeconds = 1;
        try {
            JsonNode response = jsonMapper.readTree(exception.getResponseBodyAsString());
            retryAfterSeconds = Math.max(1, response.path("parameters").path("retry_after").asInt(1));
        } catch (RuntimeException ignored) {
            // Keep a short pause even when Telegram does not return valid retry parameters.
        }
        long now = System.nanoTime();
        long delay = TimeUnit.SECONDS.toNanos(retryAfterSeconds);
        if (retryAllowedAtNanos == 0 || retryAllowedAtNanos - now < delay) {
            retryAllowedAtNanos = now + delay;
        }
    }

    List<String> splitMessage(String text) {
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
