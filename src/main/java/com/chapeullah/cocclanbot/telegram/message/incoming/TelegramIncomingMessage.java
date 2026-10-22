package com.chapeullah.cocclanbot.telegram.message.incoming;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity @Table(name = "telegram_message_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramIncomingMessage {

    @Id
    @Column(name = "update_id", nullable = false)
    private Long updateId;

    @Column(name = "message_id", nullable = false)
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

    @Column(name = "reply_to_message_id", nullable = true)
    private Long replyToMessageId;

    @Column(name = "reply_to_ephemeral_message_id", nullable = true)
    private Integer replyToEphemeralMessageId;

    @Column(name = "text", nullable = true, columnDefinition = "text")
    private String text;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "raw_json", nullable = false, columnDefinition = "text")
    private String rawJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TelegramIncomingMessage(
            @NonNull Long updateId,
            @NonNull Long messageId,
            @NonNull Long chatId,
            Integer threadId,
            Long userId,
            String username,
            String firstName,
            String lastName,
            String text,
            @NonNull Instant sentAt,
            @NonNull String rawJson) {
        this.updateId = updateId;
        this.messageId = messageId;
        this.chatId = chatId;
        this.threadId = threadId;
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.text = text;
        this.sentAt = sentAt;
        this.rawJson = rawJson;
    }

    public void captureDetails(@NonNull TelegramMessage message) {
        this.ephemeralMessageId = message.ephemeralMessageId();
        this.chatType = message.telegramChat().type();
        this.chatTitle = message.telegramChat().title();
        this.chatUsername = message.telegramChat().username();
        this.userBot = message.telegramUser() == null ? null : message.telegramUser().isBot();
        var receiver = message.receiverUser();
        if (receiver != null) {
            this.receiverUserId = receiver.id();
            this.receiverUsername = receiver.username();
            this.receiverFirstName = receiver.firstName();
            this.receiverLastName = receiver.lastName();
        }
        var reply = message.replyToMessage();
        if (reply != null) {
            this.replyToMessageId = reply.messageId();
            this.replyToEphemeralMessageId = reply.ephemeralMessageId();
        }
    }
}
