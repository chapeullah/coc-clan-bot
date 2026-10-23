package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetMainThreadCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    @Override
    public String getName() {
        return "/reset_main_thread";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Сбросить основной чат и разрешить в нём команды";
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return telegramCommandService.resetMainThread(message.telegramChat().id(), userId);
    }
}
