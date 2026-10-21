package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.war.model.War;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity @Table(
        name = "telegram_war_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_telegram_war_notification_war_id_type",
                columnNames = {"war_id", "type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramWarNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "war_id", nullable = false)
    private War war;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TelegramWarNotificationType type;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "chat_id_changed_at", nullable = true)
    private Instant chatIdChangedAt;

    @Column(name = "thread_id", nullable = false)
    private Integer threadId;

    @Column(name = "thread_id_changed_at", nullable = true)
    private Instant threadIdChangedAt;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TelegramWarNotification(
            @NonNull War war,
            @NonNull TelegramWarNotificationType type,
            @NonNull Long chatId,
            @NonNull Integer threadId,
            @NonNull String text) {
        this.war = war;
        this.type = type;
        this.chatId = chatId;
        this.threadId = threadId;
        this.text = text;
    }

    public void update(@NonNull Long chatId, @NonNull Integer threadId) {
        ensureNotSent();

        Instant now = Instant.now();

        if (!Objects.equals(this.chatId, chatId)) {
            this.chatId = chatId;
            this.chatIdChangedAt = now;
        }

        if (!Objects.equals(this.threadId, threadId)) {
            this.threadId = threadId;
            this.threadIdChangedAt = now;
        }
    }

    public void setChatId(@NonNull Long chatId) {
        ensureNotSent();

        if (!Objects.equals(this.chatId, chatId)) {
            this.chatId = chatId;
            this.chatIdChangedAt = Instant.now();
        }
    }

    public void setThreadId(@NonNull Integer threadId) {
        ensureNotSent();

        if (!Objects.equals(this.threadId, threadId)) {
            this.threadId = threadId;
            this.threadIdChangedAt = Instant.now();
        }
    }

    public void markSent() {
        ensureNotSent();
        this.sentAt = Instant.now();
    }

    private void ensureNotSent() {
        if (this.sentAt != null) {
            throw new IllegalStateException("Cannot update a sent notification");
        }
    }

}
