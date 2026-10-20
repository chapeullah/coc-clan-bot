package com.chapeullah.guccigoblin.telegram.command;

import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;

public interface BotCommand {

    String getName();

    String getDescription();

    String execute(String args, TelegramMessage message);

}
