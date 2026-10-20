package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.telegram.TelegramClient;
import com.chapeullah.guccigoblin.telegram.TelegramUtils;
import lombok.NonNull;
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

    /*
    @Value("${TELEGRAM_GROUP_CHAT_ID}")
    private long groupChatId;

    @Value("${TELEGRAM_WAR_THREAD_ID}")
    private Integer warThreadId;

    @Value("${TELEGRAM_RAID_THREAD_ID}")
    private Integer raidThreadId;

    public void sendTestMessages() {
        sendThreadMessage(warThreadId, "⚔️ Тестовое сообщение в теме КВ");
        sendThreadMessage(raidThreadId, "🏰 Тестовое сообщение в теме рейдов");
    }

    public void sendThreadMessage(@NonNull Integer threadId, @NonNull String text) {
        telegramClient.sendMessage(groupChatId, threadId, text);
    }

     */

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
