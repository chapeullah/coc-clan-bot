package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetGroupCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    @Override
    public String getName() {
        return "/set_group";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Назначить текущую группу группой клана";
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null
                ? null
                : message.telegramUser().id();

        return telegramCommandService.setGroup(message.telegramChat().id(), userId);
    }
}
