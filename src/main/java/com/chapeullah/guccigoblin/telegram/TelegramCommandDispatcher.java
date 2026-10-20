package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
public class TelegramCommandDispatcher {

    private final Map<String, BiFunction<String, Long, String>> commands;

    public TelegramCommandDispatcher(List<BotCommand> commands) {
        this.commands = commands.stream()
                .collect(Collectors.toMap(
                        BotCommand::getName,
                        command -> (BiFunction<String, Long, String>) command::execute));
    }

    public String execute(String commandName, String args, long chatId) {
        BiFunction<String, Long, String> command = commands.get(commandName);
        if (command == null) {
            return "Команда не найдена";
        }
        return command.apply(args, chatId);
    }

}
