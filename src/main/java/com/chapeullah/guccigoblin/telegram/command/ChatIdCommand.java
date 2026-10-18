package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatIdCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/chatid";
    private static final String DESCRIPTION = "ID текущего чата";

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
        return telegramCommandService.getChatId(chatId);
    }

}
