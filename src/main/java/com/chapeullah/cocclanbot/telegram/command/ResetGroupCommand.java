package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetGroupCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    @Override
    public String getName() {
        return "/reset_group";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Сбросить группу клана и все настройки тем";
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return telegramCommandService.resetGroup(message.telegramChat().id(), userId);
    }
}
