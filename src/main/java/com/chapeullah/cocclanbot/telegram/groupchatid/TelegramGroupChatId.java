package com.chapeullah.cocclanbot.telegram.groupchatid;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity @Table(name = "telegram_chat_id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramGroupChatId {

    public static final Long SETTING_ID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "chat_id", nullable = true)
    private Long chatId;

    @Column(name = "chat_id_changed_at", nullable = true)
    private Instant chatIdChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updateAt;

    public TelegramGroupChatId(Long chatId) {
        this.id = SETTING_ID;
        this.chatId = chatId;
    }

    public void setChatId(Long chatId) {
        if (!Objects.equals(this.chatId, chatId)) {
            this.chatId = chatId;
            this.chatIdChangedAt = Instant.now();
        }
    }

}
