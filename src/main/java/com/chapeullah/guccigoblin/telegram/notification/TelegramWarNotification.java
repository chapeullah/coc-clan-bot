package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.war.model.War;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

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
    private WarNotificationType type;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "thread_id", nullable = true)
    private Integer threadId;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TelegramWarNotification(
            @NonNull War war,
            @NonNull WarNotificationType type,
            @NonNull Long chatId,
            Integer threadId,
            @NonNull String text) {
        this.war = war;
        this.type = type;
        this.chatId = chatId;
        this.threadId = threadId;
        this.text = text;
    }

    public void markSent() {
        this.sentAt = Instant.now();
    }

    public void setThreadId(@NonNull Integer threadId) {
        if (sentAt != null) {
            throw new IllegalStateException("Cannot change the thread of a sent notification");
        }
        this.threadId = threadId;
    }

}
