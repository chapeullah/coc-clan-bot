package com.chapeullah.cocclanbot.telegram.message.outgoing;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramOutgoingMessageService {

    private final TelegramOutgoingMessageRepository telegramOutgoingMessageRepository;
    private final TelegramIncomingMessageRepository telegramIncomingMessageRepository;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public Optional<Instant> findWarNotificationSentAt(Long notificationId) {
        return telegramOutgoingMessageRepository.findWarNotificationSentAt(notificationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long startAttempt(TelegramOutgoingMessageContext context, long chatId, Integer threadId,
                             String text, String parseMode, boolean ephemeral,
                             Integer replyToEphemeralMessageId, String requestJson) {
        var source = context.sourceUpdateId() == null ? null
                : telegramIncomingMessageRepository.getReferenceById(context.sourceUpdateId());
        int attemptNumber = Math.toIntExact(telegramOutgoingMessageRepository
                .countByDeliveryIdAndPartNumber(context.deliveryId(), context.partNumber()) + 1);
        var message = new TelegramOutgoingMessage(context, source, attemptNumber, chatId, threadId,
                text, parseMode, ephemeral, replyToEphemeralMessageId, requestJson);
        return telegramOutgoingMessageRepository.save(message).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long id, TelegramMessage message, int httpStatus,
                         String rawJson, String responseHeadersJson) {
        getAttempt(id).markSent(message, httpStatus, rawJson, responseHeadersJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, TelegramOutgoingMessageStatus status, Integer httpStatus,
                           String rawJson, String responseHeadersJson, String errorType, String errorMessage) {
        JsonNode response = null;
        if (rawJson != null && !rawJson.isBlank()) {
            try {
                response = jsonMapper.readTree(rawJson);
            } catch (RuntimeException ignored) {
            }
        }
        JsonNode parameters = response == null ? null : response.path("parameters");
        getAttempt(id).markFailed(status, httpStatus, rawJson, responseHeadersJson, errorType, errorMessage,
                integer(response, "error_code"), string(response, "description"),
                integer(parameters, "retry_after"), longValue(parameters, "migrate_to_chat_id"));
    }

    private TelegramOutgoingMessage getAttempt(Long id) {
        return telegramOutgoingMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outgoing Telegram attempt not found: id=" + id));
    }

    private static Integer integer(JsonNode node, String field) {
        return node != null && node.path(field).isIntegralNumber() ? node.path(field).asInt() : null;
    }

    private static Long longValue(JsonNode node, String field) {
        return node != null && node.path(field).isIntegralNumber() ? node.path(field).asLong() : null;
    }

    private static String string(JsonNode node, String field) {
        return node != null && node.path(field).isString() ? node.path(field).asString() : null;
    }
}
