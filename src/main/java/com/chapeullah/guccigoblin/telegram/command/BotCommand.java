package com.chapeullah.guccigoblin.telegram.command;

public interface BotCommand {

    String getName();

    String getDescription();

    String execute(String args, long chatId);

}
