package com.chapeullah.cocclanbot.telegram.command;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BotCommandService {

    private final Map<BotCommandType, List<BotCommand>> commandsByType;

    public BotCommandService(List<BotCommand> commands) {
        this.commandsByType = commands
                .stream()
                .sorted(Comparator.comparing(BotCommand::getName))
                .collect(Collectors.groupingBy(
                        BotCommand::getType,
                        () -> new EnumMap<>(BotCommandType.class),
                        Collectors.toList()));
    }

    public Map<BotCommandType, List<BotCommand>> getCommandsByType() {
        return this.commandsByType;
    }

}
