package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.war.model.WarAttack;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity @Table(name = "telegram_war_attack_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramWarAttackNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "war_attack_id", nullable = false)
    private WarAttack warAttack;

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

    public TelegramWarAttackNotification(
            @NonNull WarAttack warAttack,
            @NonNull Long chatId,
            Integer threadId,
            @NonNull String text) {
        this.warAttack = warAttack;
        this.chatId = chatId;
        this.threadId = threadId;
        this.text = text;
    }

    public void markSent() {
        this.sentAt = Instant.now();
    }

}
