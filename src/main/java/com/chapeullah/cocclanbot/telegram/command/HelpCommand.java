package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HelpCommand implements BotCommand {

    private static final String NAME = "/help";
    private static final String DESCRIPTION = "Список команд";
    private static final BotCommandType TYPE = BotCommandType.GENERAL;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final ObjectProvider<BotCommandService> botCommandService;

    private final TelegramAccessService telegramAccessService;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    @Transactional(readOnly = true)
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        if (userId == null) {
            throw new IllegalStateException("Command sender is missing: chatId=" + message.telegramChat().id()
                    + ", messageId=" + message.messageId());
        }
        boolean isAdmin = telegramAccessService.isAdmin(userId);
        boolean isPrivateChat = Objects.equals("private", message.telegramChat().type());
        return botCommandMessageService.getHelpMessage(
                botCommandService.getObject().getCommandsByType(),
                isAdmin,
                isPrivateChat);
    }

}
