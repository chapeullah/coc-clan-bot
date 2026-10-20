package com.chapeullah.guccigoblin.telegram;

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
public class TelegramMessage {

    @Id
    @Column(name = "update_id", nullable = false)
    private Long updateId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

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

    @Column(name = "text", nullable = true, columnDefinition = "text")
    private String text;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "raw_json", nullable = false, columnDefinition = "text")
    private String rawJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TelegramMessage(
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
}
