package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetWarThreadCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    @Override
    public String getName() {
        return "/reset_war_thread";
    }

    @Override
    public String getDescription() {
        return "Сбросить тему уведомлений о КВ";
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return telegramCommandService.resetWarThread(message.telegramChat().id(), userId);
    }
}
