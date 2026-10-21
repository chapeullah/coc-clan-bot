package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TelegramCommandDispatcher {

    private final Map<String, BotCommand> commands;
    private final TelegramAccessService telegramAccessService;

    public TelegramCommandDispatcher(List<BotCommand> commands, TelegramAccessService telegramAccessService) {
        this.telegramAccessService = telegramAccessService;
        this.commands = commands.stream()
                .collect(Collectors.toMap(
                        BotCommand::getName,
                        Function.identity()));
    }

    public String execute(String commandName, String args, TelegramMessage message) {
        if (!telegramAccessService.canExecute(commandName, message)) {
            return "Бот работает только в группе клана.\n"
                    + "В личных сообщениях доступны только /start и /help.\n"
                    + "В группе ваши команды боту и его ответы видны только вам.";
        }
        BotCommand command = commands.get(commandName);
        if (command == null) {
            return "Команда не найдена";
        }
        return command.execute(args, message);
    }

    public List<BotCommand> getCommands() {
        return commands.values().stream()
                .sorted(Comparator.comparing(BotCommand::getName))
                .toList();
    }

}
