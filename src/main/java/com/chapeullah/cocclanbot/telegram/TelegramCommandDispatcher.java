package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.BotCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
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
            String response = "Бот работает только в группе клана.\n"
                    + "В личных сообщениях доступны только /start и /help.";
            if (message != null && message.telegramChat() != null
                    && "private".equals(message.telegramChat().type())) {
                response += "\n\n<i>В группе ваши команды и ответы бота видны только вам.</i>";
            }
            return response;
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
