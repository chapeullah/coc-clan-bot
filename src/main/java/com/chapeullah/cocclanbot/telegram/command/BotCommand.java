package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;

public interface BotCommand {

    String getName();

    String getDescription();

    String execute(String args, TelegramMessage message);

}
