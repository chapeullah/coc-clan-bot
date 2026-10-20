package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarThreadCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/war_thread";
    private static final String DESCRIPTION = "Назначить текущую тему для уведомлений";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null
                ? null
                : message.telegramUser().id();

        return telegramCommandService.setWarThread(
                message.telegramChat().id(),
                userId,
                message.threadId());
    }
}
