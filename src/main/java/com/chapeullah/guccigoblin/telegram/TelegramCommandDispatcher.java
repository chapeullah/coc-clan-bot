package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TelegramCommandDispatcher {

    private final Map<String, BotCommand> commands;

    public TelegramCommandDispatcher(List<BotCommand> commands) {
         this.commands = commands.stream()
                 .collect(Collectors.toMap(
                         BotCommand::getName,
                         Function.identity()));
    }

    public String execute(String commandName, String args, long chatId) {
        BotCommand command = commands.get(commandName);
        if (command == null) {
            return "Команда не найдена";
        }
        return command.execute(args, chatId);
    }

}
