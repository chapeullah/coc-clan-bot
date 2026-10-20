package com.chapeullah.guccigoblin.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "telegram_chat_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramChatSetting {

    @Id
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "war_thread_id", nullable = false)
    private Integer warThreadId;

    public TelegramChatSetting(
            @NonNull Long chatId,
            @NonNull Integer warThreadId) {
        this.chatId = chatId;
        this.warThreadId = warThreadId;
    }

    public void changeWarThread(@NonNull Integer warThreadId) {
        this.warThreadId = warThreadId;
    }
}