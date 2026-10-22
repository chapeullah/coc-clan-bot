package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChatIdCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    private static final String NAME = "/chat_id";
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
    public String execute(String args, TelegramMessage message) {
        return telegramCommandService.getChatId(message.telegramChat().id());
    }

}
