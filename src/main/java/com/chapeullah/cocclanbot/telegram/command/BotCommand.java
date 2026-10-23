package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;

public interface BotCommand {

    String getName();

    String getDescription();

    default boolean isAdminOnly() {
        return false;
    }

    String execute(String args, TelegramMessage message);

    default TelegramCommandReply executeReply(String args, TelegramMessage message) {
        return TelegramCommandReply.privateReply(execute(args, message));
    }

}
