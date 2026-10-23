package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;

public interface BotCommand {

    String getName();

    String getDescription();

    default boolean isAdminOnly() {
        return false;
    }

    String execute(String args, TelegramMessage message);

}
