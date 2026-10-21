package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.guccigoblin.telegram.TelegramClient;
import com.chapeullah.guccigoblin.telegram.TelegramUtils;
import com.chapeullah.guccigoblin.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.guccigoblin.war.model.War;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class TelegramWarNotificationService {

    private final TelegramClient telegramClient;

    private final TelegramWarNotificationRepository telegramWarNotificationRepository;

    private final TelegramGroupChatIdService telegramGroupChatIdService;
    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;

    @Value("${TELEGRAM_SYSTEM_CHAT_ID}")
    private long systemChatId;

    private static final Map<TelegramWarNotificationType, String> WAR_NOTIFICATION_TYPES = Map.of(
            TelegramWarNotificationType.PREPARATION, "⏳ Подготовка к войне",
            TelegramWarNotificationType.FIRST_ATTACK, "⚔️ Война началась",
            TelegramWarNotificationType.SECOND_ATTACK, "🔔 Напоминание о второй атаке");

    public void sendError(String operation, Exception exception) {
        try {
            telegramClient.sendMessage(systemChatId, null,
                    TelegramUtils.escapeHtml("❌ ERROR: " + operation + "\n\n" + exception),
                    TelegramOutgoingMessageContext.systemError(operation));
        } catch (Exception e) {
            log.warn("Failed to send Telegram error notification: operation=\"{}\"", operation, e);
        }
    }

    public void sendWarNotification(@NonNull War war) {
        TelegramWarNotificationType type = resolveNotificationType(war).orElse(null);
        if (type == null) {
            log.debug("War has ended, skipping notification: warId={}, state={}", war.getId(), war.getState());
            return;
        }

        TelegramWarNotification notification = telegramWarNotificationRepository
                .findByWarAndType(war, type)
                .orElse(null);
        if (notification != null && notification.getSentAt() != null) {
            log.debug("War notification already sent, skipping: warId={}, type={}", war.getId(), type);
            return;
        }

        Long chatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (chatId == null) {
            log.debug("Telegram group chat ID is not configured, skipping war notification: warId={}", war.getId());
            return;
        }

        Integer threadId = telegramGroupChatThreadsService.getWarThreadId().orElse(null);
        if (threadId == null) {
            log.debug("Telegram group war thread ID is not configured, skipping war notification: warId={}", war.getId());
            return;
        }

        if (notification == null) {
            notification = new TelegramWarNotification(
                    war,
                    type,
                    chatId,
                    threadId,
                    getWarNotificationMessage(war, type));
        } else if (!Objects.equals(notification.getChatId(), chatId) || !Objects.equals(notification.getThreadId(), threadId)) {
            notification.update(chatId, threadId);
        }
        notification = telegramWarNotificationRepository.save(notification);

        telegramClient.sendMessage(
                notification.getChatId(),
                notification.getThreadId(),
                notification.getText(),
                TelegramOutgoingMessageContext.warNotification(notification.getId()));

        notification.markSent();
        telegramWarNotificationRepository.save(notification);
        log.info("War notification sent successfully: warId={}, type={}", war.getId(), type);
    }

    private String getWarNotificationMessage(@NonNull War war, TelegramWarNotificationType type) {
        String clanName = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(war.getClanName()));
        String opponentClanName = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(war.getOpponentClanName()));
        return WAR_NOTIFICATION_TYPES.get(type) + " | " + clanName + " — " + opponentClanName;
    }

    public Optional<TelegramWarNotificationType> resolveNotificationType(War war) {
        Instant now = Instant.now();
        if (war == null
                || Objects.equals("warEnded",war.getState())
                || !now.isBefore(war.getEndTime())) {
            return Optional.empty();
        }

        if (now.isBefore(war.getStartTime())) {
            return Optional.of(TelegramWarNotificationType.PREPARATION);
        }
        if (now.isBefore(war.getStartTime().plus(Duration.ofHours(12)))) {
            return Optional.of(TelegramWarNotificationType.FIRST_ATTACK);
        }
        return Optional.of(TelegramWarNotificationType.SECOND_ATTACK);
    }

}
