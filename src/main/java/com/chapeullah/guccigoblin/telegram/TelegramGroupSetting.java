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
@Table(name = "telegram_group_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramGroupSetting {

    public static final long SETTINGS_ID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    public TelegramGroupSetting(@NonNull Long chatId) {
        this.id = SETTINGS_ID;
        this.chatId = chatId;
    }
}
