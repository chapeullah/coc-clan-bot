package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetMainThreadCommand implements BotCommand {

    private static final String NAME = "/set_main_thread";
    private static final String DESCRIPTION = "Назначить текущий чат основным и запретить в нём команды";
    private static final BotCommandType TYPE = BotCommandType.SETTINGS;
    private static final boolean ADMIN_ONLY = true;

    private final BotCommandMessageService botCommandMessageService;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return botCommandMessageService.setMainThread(
                message.telegramChat().id(),
                userId,
                message.forumThreadId());
    }

}
