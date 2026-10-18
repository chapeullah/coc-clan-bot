package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RaidCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/raid";
    private static final String DESCRIPTION = "Текущий сезон рейдов";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String execute(String args, long chatId) {
        return telegramCommandService.getRaidMessage();
    }

}
