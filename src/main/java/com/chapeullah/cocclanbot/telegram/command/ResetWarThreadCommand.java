package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class ResetWarThreadCommand implements BotCommand {

    private static final String NAME = "/reset_war_thread";
    private static final String DESCRIPTION = "Сбросить тему уведомлений о КВ";
    private static final BotCommandType TYPE = BotCommandType.SETTINGS;
    private static final boolean ADMIN_ONLY = true;

    private final BotCommandMessageService botCommandMessageService;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return botCommandMessageService.resetWarThread(message.telegramChat().id(), userId);
    }
}
