package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarsCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/wars";
    private static final String DESCRIPTION = "История войн клана";

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
        return telegramCommandService.getWarsMessage();
    }

}
