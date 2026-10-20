package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingRaidsCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/rating_raids";
    private static final String DESCRIPTION = "Рейтинг и статистика рейдов";

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
        return telegramCommandService.getRaidRatingMessage();
    }

}
