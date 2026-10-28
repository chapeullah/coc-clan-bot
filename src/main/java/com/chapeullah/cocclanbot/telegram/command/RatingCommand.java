package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingCommand implements BotCommand {

    private static final String NAME = "/rating";
    private static final String DESCRIPTION = "Общий рейтинг участников клана";
    private static final BotCommandType TYPE = BotCommandType.RATING;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        return botCommandMessageService.getRatingMessage(cocProperties.getClanTag());
    }

}
