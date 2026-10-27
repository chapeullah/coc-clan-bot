package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class RatingCommand implements BotCommand {

    private static final String NAME = "/rating";
    private static final String DESCRIPTION = "Общий рейтинг участников клана";
    private static final BotCommandType TYPE = BotCommandType.RATING;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        return botCommandMessageService.getRatingMessage();
    }

}
