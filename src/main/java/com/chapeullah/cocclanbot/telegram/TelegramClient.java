package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.BotCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramApiResponse;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageStatus;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@DependsOn("environmentVariablesService")
public class TelegramClient {

    private static final int MESSAGE_LENGTH_LIMIT = 4000;
    private static final Duration EPHEMERAL_REPLY_TIMEOUT = Duration.ofSeconds(15);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final TelegramOutgoingMessageService telegramOutgoingMessageService;
    private final Clock clock;

    private String username;
    private volatile long retryAllowedAtNanos;
    private final Map<Map<String, Object>, Boolean> configuredChatMenus = new HashMap<>();

    @Autowired
    public TelegramClient(
            @Value("${TELEGRAM_BOT_TOKEN}") String token, JsonMapper jsonMapper,
            TelegramOutgoingMessageService telegramOutgoingMessageService) {
        this(createRestClient(token), jsonMapper, telegramOutgoingMessageService);
    }

    TelegramClient(
            RestClient restClient, JsonMapper jsonMapper,
            TelegramOutgoingMessageService telegramOutgoingMessageService) {
        this(restClient, jsonMapper, telegramOutgoingMessageService, Clock.systemUTC());
    }

    TelegramClient(
            RestClient restClient, JsonMapper jsonMapper,
            TelegramOutgoingMessageService telegramOutgoingMessageService, Clock clock) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.telegramOutgoingMessageService = telegramOutgoingMessageService;
        this.clock = clock;
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
        List<BotCommand> userCommands = commands.stream().filter(command -> !command.isAdminOnly()).toList();
        setCommands(userCommands, Map.of("type", "default"));
        setCommands(userCommands, Map.of("type", "all_group_chats"));
        setCommands(commands.stream().filter(command -> TelegramAccessService.isPublicCommand(command.getName())).toList(),
                Map.of("type", "all_private_chats"));
    }

    public synchronized void configureChatCommands(List<BotCommand> commands, TelegramMessage message, boolean admin) {
        if (message.telegramUser() == null) {
            return;
        }
        boolean privateChat = "private".equals(message.telegramChat().type());
        if (!privateChat && !"group".equals(message.telegramChat().type())
                && !"supergroup".equals(message.telegramChat().type())) {
            return;
        }
        Map<String, Object> scope = privateChat
                ? Map.of("type", "chat", "chat_id", message.telegramChat().id())
                : Map.of("type", "chat_member", "chat_id", message.telegramChat().id(),
                         "user_id", message.telegramUser().id());
        if (Objects.equals(configuredChatMenus.get(scope), admin)) {
            return;
        }
        List<BotCommand> visibleCommands = commands.stream()
                .filter(command -> admin || (!command.isAdminOnly()
                        && (!privateChat || TelegramAccessService.isPublicCommand(command.getName()))))
                .toList();
        setCommands(visibleCommands, scope);
        configuredChatMenus.put(scope, admin);
    }

    private void setCommands(List<BotCommand> commands, Map<String, Object> scope) {
        List<Map<String, Object>> menu = commands.stream()
                .map(command -> Map.<String, Object>of(
                        "command", command.getName().substring(1),
                        "description", command.getDescription(),
                        "is_ephemeral", true))
                .toList();
        String requestJson = jsonMapper.writeValueAsString(Map.of(
                "commands", menu,
                "scope", scope));
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
            sendMessagePart(chatId, threadId, null, Map.of(), parts.get(index),
                    context.part(index + 1, parts.size()));
        }
    }

    void sendReplyPart(TelegramMessage message, String text, TelegramOutgoingMessageContext context) {
        long chatId = message.telegramChat().id();
        Long receiverUserId = chatId < 0 ? message.telegramUser().id() : null;
        Map<String, Object> replyParameters = buildReplyParameters(message);
        try {
            sendMessagePart(chatId, message.forumThreadId(), receiverUserId, replyParameters, text, context);
        } catch (RestClientResponseException exception) {
            if (replyParameters.isEmpty() || !isInvalidReply(exception)) {
                throw exception;
            }
            log.debug("Telegram reply target is unavailable, sending without it: chatId={}", chatId);
            sendMessagePart(chatId, message.forumThreadId(), receiverUserId, Map.of(), text, context);
        }
    }

    private Map<String, Object> buildReplyParameters(TelegramMessage message) {
        if (message.ephemeralMessageId() != null) {
            if (message.date() > 0 && clock.instant().isBefore(
                    Instant.ofEpochSecond(message.date()).plus(EPHEMERAL_REPLY_TIMEOUT))) {
                return Map.of("ephemeral_message_id", message.ephemeralMessageId());
            }
            return Map.of();
        }
        return message.messageId() > 0 ? Map.of("message_id", message.messageId()) : Map.of();
    }

    private boolean isInvalidReply(RestClientResponseException exception) {
        if (exception.getStatusCode().value() != 400) {
            return false;
        }
        try {
            JsonNode response = jsonMapper.readTree(exception.getResponseBodyAsString());
            if (response == null) {
                return false;
            }
            String description = response.path("description").asString("").toLowerCase(Locale.ROOT);
            return description.contains("reply_to_invalid")
                    || description.contains("reply_message_id_invalid")
                    || description.contains("message to be replied not found")
                    || description.contains("message to reply not found");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void sendMessagePart(long chatId, Integer threadId, Long receiverUserId, Map<String, Object> replyParameters,
                                 String text, TelegramOutgoingMessageContext context) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        body.put("disable_notification", false);
        if (threadId != null) {
            body.put("message_thread_id", threadId);
        }
        if (receiverUserId != null) {
            body.put("ephemeral_message_parameters", Map.of("receiver_user_id", receiverUserId));
        }
        if (!replyParameters.isEmpty()) {
            body.put("reply_parameters", replyParameters);
        }
        Integer ephemeralMessageId = (Integer) replyParameters.get("ephemeral_message_id");
        String requestJson = jsonMapper.writeValueAsString(body);
        executeRequest(() -> {
            sendAndRecord(context, chatId, threadId, receiverUserId != null, ephemeralMessageId, text, requestJson);
            return null;
        });
    }

    private void sendAndRecord(
            TelegramOutgoingMessageContext context,
            long chatId,
            Integer threadId,
            boolean ephemeral,
            Integer ephemeralMessageId,
            String text,
            String requestJson) {
        Long attemptId = telegramOutgoingMessageService.startAttempt(context, chatId, threadId,
                text, "HTML", ephemeral, ephemeralMessageId, requestJson);
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
            throw new RestClientResponseException(body.path("description").asString("Telegram rejected the message"),
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

    private void recordFailure(
            Long attemptId,
            TelegramOutgoingMessageStatus status,
            Integer httpStatus,
            String rawJson,
            String headersJson,
            RuntimeException failure) {
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
