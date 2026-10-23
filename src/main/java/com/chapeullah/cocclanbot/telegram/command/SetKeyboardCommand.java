package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.telegram.TelegramCommandService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetKeyboardCommand implements BotCommand {

    private final TelegramCommandService telegramCommandService;

    @Override
    public String getName() {
        return "/set_keyboard";
    }

    @Override
    public String getDescription() {
        return "Показать клавиатуру бота в текущем чате";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public String execute(String args, TelegramMessage message) {
        return executeReply(args, message).text();
    }

    @Override
    public TelegramCommandReply executeReply(String args, TelegramMessage message) {
        return telegramCommandService.getKeyboardReply(args, message);
    }
}
