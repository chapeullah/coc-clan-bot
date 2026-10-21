package com.chapeullah.guccigoblin.telegram.message.outgoing;

import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.message.incoming.TelegramIncomingMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_outgoing_message_history", indexes = {
        @Index(name = "idx_telegram_outgoing_source_update", columnList = "source_update_id"),
        @Index(name = "idx_telegram_outgoing_delivery_part", columnList = "delivery_id, part_number"),
        @Index(name = "idx_telegram_outgoing_chat_created", columnList = "chat_id, created_at"),
        @Index(name = "idx_telegram_outgoing_notification", columnList = "notification_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramOutgoingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TelegramOutgoingMessageType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_update_id", nullable = true)
    private TelegramIncomingMessage sourceMessage;

    @Column(name = "notification_id", nullable = true)
    private Long notificationId;

    @Column(name = "operation", nullable = true, columnDefinition = "text")
    private String operation;

    @Column(name = "part_number", nullable = false)
    private int partNumber;

    @Column(name = "part_count", nullable = false)
    private int partCount;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "message_id", nullable = true)
    private Long messageId;

    @Column(name = "ephemeral_message_id", nullable = true)
    private Integer ephemeralMessageId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "chat_type", nullable = true)
    private String chatType;

    @Column(name = "chat_title", nullable = true, length = 1024)
    private String chatTitle;

    @Column(name = "chat_username", nullable = true)
    private String chatUsername;

    @Column(name = "chat_first_name", nullable = true)
    private String chatFirstName;

    @Column(name = "chat_last_name", nullable = true)
    private String chatLastName;

    @Column(name = "thread_id", nullable = true)
    private Integer threadId;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "username", nullable = true)
    private String username;

    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Column(name = "last_name", nullable = true)
    private String lastName;

    @Column(name = "user_is_bot", nullable = true)
    private Boolean userBot;

    @Column(name = "receiver_user_id", nullable = true)
    private Long receiverUserId;

    @Column(name = "receiver_username", nullable = true)
    private String receiverUsername;

    @Column(name = "receiver_first_name", nullable = true)
    private String receiverFirstName;

    @Column(name = "receiver_last_name", nullable = true)
    private String receiverLastName;

    @Column(name = "is_ephemeral", nullable = false)
    private boolean ephemeral;

    @Column(name = "reply_to_message_id", nullable = true)
    private Long replyToMessageId;

    @Column(name = "reply_to_ephemeral_message_id", nullable = true)
    private Integer replyToEphemeralMessageId;

    @Column(name = "text", nullable = true, columnDefinition = "text")
    private String text;

    @Column(name = "request_text", nullable = false, columnDefinition = "text")
    private String requestText;

    @Column(name = "parse_mode", nullable = false)
    private String parseMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TelegramOutgoingMessageStatus status;

    @Column(name = "http_status", nullable = true)
    private Integer httpStatus;

    @Column(name = "api_error_code", nullable = true)
    private Integer apiErrorCode;

    @Column(name = "api_description", nullable = true, columnDefinition = "text")
    private String apiDescription;

    @Column(name = "error_type", nullable = true)
    private String errorType;

    @Column(name = "error_message", nullable = true, columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_after_seconds", nullable = true)
    private Integer retryAfterSeconds;

    @Column(name = "migrate_to_chat_id", nullable = true)
    private Long migrateToChatId;

    @Column(name = "request_json", nullable = false, columnDefinition = "text")
    private String requestJson;

    @Column(name = "raw_json", nullable = true, columnDefinition = "text")
    private String rawJson;

    @Column(name = "response_headers_json", nullable = true, columnDefinition = "text")
    private String responseHeadersJson;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    public TelegramOutgoingMessage(
            @NonNull TelegramOutgoingMessageContext context,
            TelegramIncomingMessage sourceMessage,
            int attemptNumber,
            long chatId,
            Integer threadId,
            @NonNull String requestText,
            @NonNull String parseMode,
            boolean ephemeral,
            Integer replyToEphemeralMessageId,
            @NonNull String requestJson) {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("Attempt number must be positive");
        }
        this.deliveryId = context.deliveryId();
        this.type = context.type();
        this.sourceMessage = sourceMessage;
        this.notificationId = context.notificationId();
        this.operation = context.operation();
        this.partNumber = context.partNumber();
        this.partCount = context.partCount();
        this.attemptNumber = attemptNumber;
        this.chatId = chatId;
        this.threadId = threadId;
        this.requestText = requestText;
        this.parseMode = parseMode;
        this.ephemeral = ephemeral;
        this.replyToEphemeralMessageId = replyToEphemeralMessageId;
        this.requestJson = requestJson;
        this.status = TelegramOutgoingMessageStatus.PENDING;
        captureReceiver(context.receiver());
    }

    public void markSent(@NonNull TelegramMessage message, int httpStatus,
                         @NonNull String rawJson, String responseHeadersJson) {
        ensurePending();
        this.messageId = message.messageId();
        this.ephemeralMessageId = message.ephemeralMessageId();
        this.chatId = message.telegramChat().id();
        this.chatType = message.telegramChat().type();
        this.chatTitle = message.telegramChat().title();
        this.chatUsername = message.telegramChat().username();
        this.chatFirstName = message.telegramChat().firstName();
        this.chatLastName = message.telegramChat().lastName();
        if (message.threadId() != null) {
            this.threadId = message.threadId();
        }
        var sender = message.telegramUser();
        if (sender != null) {
            this.userId = sender.id();
            this.username = sender.username();
            this.firstName = sender.firstName();
            this.lastName = sender.lastName();
            this.userBot = sender.isBot();
        }
        captureReceiver(message.receiverUser());
        if (message.replyToMessage() != null) {
            this.replyToMessageId = message.replyToMessage().messageId();
            this.replyToEphemeralMessageId = message.replyToMessage().ephemeralMessageId();
        }
        this.text = message.text();
        this.sentAt = Instant.ofEpochSecond(message.date());
        this.status = TelegramOutgoingMessageStatus.SENT;
        complete(httpStatus, rawJson, responseHeadersJson);
    }

    public void markFailed(@NonNull TelegramOutgoingMessageStatus status,
                           Integer httpStatus, String rawJson, String responseHeadersJson,
                           String errorType, String errorMessage, Integer apiErrorCode,
                           String apiDescription, Integer retryAfterSeconds, Long migrateToChatId) {
        ensurePending();
        if (status != TelegramOutgoingMessageStatus.FAILED && status != TelegramOutgoingMessageStatus.UNKNOWN) {
            throw new IllegalArgumentException("Expected FAILED or UNKNOWN status");
        }
        this.status = status;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.apiErrorCode = apiErrorCode;
        this.apiDescription = apiDescription;
        this.retryAfterSeconds = retryAfterSeconds;
        this.migrateToChatId = migrateToChatId;
        complete(httpStatus, rawJson, responseHeadersJson);
    }

    private void captureReceiver(TelegramUser receiver) {
        if (receiver != null) {
            this.receiverUserId = receiver.id();
            this.receiverUsername = receiver.username();
            this.receiverFirstName = receiver.firstName();
            this.receiverLastName = receiver.lastName();
        }
    }

    private void complete(Integer httpStatus, String rawJson, String responseHeadersJson) {
        this.httpStatus = httpStatus;
        this.rawJson = rawJson;
        this.responseHeadersJson = responseHeadersJson;
        this.completedAt = Instant.now();
    }

    private void ensurePending() {
        if (status != TelegramOutgoingMessageStatus.PENDING) {
            throw new IllegalStateException("Outgoing attempt is already completed");
        }
    }
}
