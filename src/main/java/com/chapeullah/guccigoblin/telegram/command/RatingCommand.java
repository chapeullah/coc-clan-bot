package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/rating";
    private static final String DESCRIPTION = "Общий рейтинг участников клана";

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
        return telegramCommandService.getRatingMessage();
    }

}
