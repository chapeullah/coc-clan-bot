package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@DependsOn("environmentVariablesService")
public class TelegramAccessService {

    private static final String ENVIRONMENT_VARIABLE = "TELEGRAM_ADMIN_USER_IDS";
    private static final Set<String> PUBLIC_COMMANDS = Set.of("/start", "/help");

    private final Set<Long> adminUserIds;
    private final TelegramGroupChatIdService telegramGroupChatIdService;
    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;

    public TelegramAccessService(@Value("${TELEGRAM_ADMIN_USER_IDS}") String value,
                                 TelegramGroupChatIdService telegramGroupChatIdService,
                                 TelegramGroupChatThreadsService telegramGroupChatThreadsService) {
        this.telegramGroupChatIdService = telegramGroupChatIdService;
        this.telegramGroupChatThreadsService = telegramGroupChatThreadsService;
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is missing or blank: " + ENVIRONMENT_VARIABLE);
        }
        try {
            this.adminUserIds = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Required environment variable must contain numeric user IDs: "
                    + ENVIRONMENT_VARIABLE, exception);
        }
        if (adminUserIds.isEmpty() || adminUserIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalStateException("Required environment variable must contain a nonempty list of positive user IDs: "
                    + ENVIRONMENT_VARIABLE);
        }
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
        if (isAdmin(userId)) {
            return true;
        }
        String chatType = message.telegramChat().type();
        if ("private".equals(chatType)) {
            return isPublicCommand(commandName);
        }
        if (!"group".equals(chatType) && !"supergroup".equals(chatType)) {
            return false;
        }
        return telegramGroupChatIdService.getChatId()
                .filter(chatId -> chatId == message.telegramChat().id())
                .isPresent();
    }

}
