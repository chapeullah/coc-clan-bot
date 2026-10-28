package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.config.properties.TelegramProperties;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TelegramAccessService {

    private static final Set<String> PUBLIC_COMMANDS = Set.of("/start", "/help");

    private final Set<Long> adminUserIds;
    private final TelegramGroupChatIdService telegramGroupChatIdService;
    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;

    public TelegramAccessService(
            TelegramProperties telegramProperties,
            TelegramGroupChatIdService telegramGroupChatIdService,
            TelegramGroupChatThreadsService telegramGroupChatThreadsService) {
        this.telegramGroupChatIdService = telegramGroupChatIdService;
        this.telegramGroupChatThreadsService = telegramGroupChatThreadsService;
        this.adminUserIds = Set.copyOf(telegramProperties.getAdminUserIds());
    }

    public boolean isAdmin(Long userId) {
        return userId != null && adminUserIds.contains(userId);
    }

    public static boolean isPublicCommand(String commandName) {
        return commandName != null && PUBLIC_COMMANDS.contains(commandName);
    }

    public boolean isCommandBlockedInMainThread(String commandName, TelegramMessage message) {
        if (message == null || message.telegramChat() == null) {
            return false;
        }

        String chatType = message.telegramChat().type();

        if (!"group".equals(chatType) && !"supergroup".equals(chatType)) {
            return false;
        }
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        if (("/set_main_thread".equals(commandName) || "/reset_main_thread".equals(commandName))
                && isAdmin(userId)) {
            return false;
        }
        return telegramGroupChatIdService.getChatId()
                .filter(chatId -> chatId == message.telegramChat().id())
                .filter(chatId -> telegramGroupChatThreadsService.isMainThread(message.forumThreadId()))
                .isPresent();
    }

    public boolean canExecute(String commandName, TelegramMessage message) {
        if (message == null || message.telegramChat() == null) {
            return false;
        }

        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();

        if (userId == null) {
            return false;
        }

        String chatType = message.telegramChat().type();
        if ("private".equals(chatType)) {
            return isPublicCommand(commandName);
        }

        if (!"group".equals(chatType) && !"supergroup".equals(chatType)) {
            return false;
        }

        if ("/set_group".equals(commandName) && isAdmin(userId)) {
            return true;
        }

        return telegramGroupChatIdService.getChatId()
                .filter(chatId -> chatId == message.telegramChat().id())
                .isPresent();
    }

}
