package com.chapeullah.guccigoblin.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final TelegramClient telegramClient;

    @Value("${TELEGRAM_ADMIN_CHAT_ID}")
    private long adminChatId;

    public void sendError(String operation, Exception exception) {
        try {
            telegramClient.sendMessage(
                    adminChatId,
                    TelegramUtils.escapeHtml("ERROR: " + operation + "\n\n" + exception));
        } catch (Exception e) {
            log.warn("Failed to send Telegram error notification: operation=\"{}\"", operation, e);
        }
    }

}
