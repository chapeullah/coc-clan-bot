package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkMemberCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/link_member";
    private static final String DESCRIPTION = "Привязать участника к Telegram";

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
        return telegramCommandService.getLinkMemberMessage(args, message);
    }

}
