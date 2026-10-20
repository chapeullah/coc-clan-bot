package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.telegram.TelegramChatSetting;
import com.chapeullah.guccigoblin.telegram.TelegramChatSettingRepository;
import com.chapeullah.guccigoblin.telegram.TelegramClient;
import com.chapeullah.guccigoblin.telegram.TelegramGroupSetting;
import com.chapeullah.guccigoblin.telegram.TelegramGroupSettingRepository;
import com.chapeullah.guccigoblin.telegram.TelegramUtils;
import com.chapeullah.guccigoblin.war.model.War;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {

    private final TelegramClient telegramClient;

    private final TelegramWarNotificationRepository telegramWarNotificationRepository;

    @Value("${TELEGRAM_ADMIN_CHAT_ID}")
    private long adminChatId;

    @Value("${TELEGRAM_GROUP_CHAT_ID:0}")
    private long groupChatId;

    @Value("${TELEGRAM_WAR_THREAD_ID}")
    private Integer warThreadId;

    @Value("${TELEGRAM_RAID_THREAD_ID}")
    private Integer raidThreadId;

    private final TelegramChatSettingRepository telegramChatSettingRepository;
    private final TelegramGroupSettingRepository telegramGroupSettingRepository;

    private long resolveGroupChatId() {
        return telegramGroupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID)
                .map(TelegramGroupSetting::getChatId)
                .orElse(groupChatId);
    }

    private Integer resolveWarThreadId(long targetChatId) {
        return telegramChatSettingRepository.findById(targetChatId)
                .map(TelegramChatSetting::getWarThreadId)
                .orElse(targetChatId == groupChatId && warThreadId != null && warThreadId > 0
                        ? warThreadId
                        : null);
    }

    public void sendTestMessages() {
        long targetChatId = resolveGroupChatId();
        if (targetChatId == 0) {
            throw new IllegalStateException("Telegram group is not configured; use /set_group");
        }
        telegramClient.sendMessage(targetChatId, resolveWarThreadId(targetChatId),
                "⚔️ Тестовое сообщение в теме КВ");
        telegramClient.sendMessage(targetChatId,
                targetChatId == groupChatId && raidThreadId != null && raidThreadId > 0
                        ? raidThreadId : null,
                "🏰 Тестовое сообщение в теме рейдов");
    }

    public void sendThreadMessage(@NonNull Integer threadId, @NonNull String text) {
        long targetChatId = resolveGroupChatId();
        if (targetChatId == 0) {
            throw new IllegalStateException("Telegram group is not configured; use /set_group");
        }
        telegramClient.sendMessage(targetChatId, threadId, text);
    }

    public void sendError(String operation, Exception exception) {
        try {
            telegramClient.sendMessage(
                    adminChatId,
                    TelegramUtils.escapeHtml("ERROR: " + operation + "\n\n" + exception));
        } catch (Exception e) {
            log.warn("Failed to send Telegram error notification: operation=\"{}\"", operation, e);
        }
    }

    public void sendWarNotification(@NonNull War war) {
        Optional<WarNotificationType> resolvedType =
                resolveNotificationType(war);

        if (resolvedType.isEmpty()) {
            log.debug(
                    "War has ended, skipping notification: warId={}, state={}",
                    war.getId(), war.getState());
            return;
        }

        WarNotificationType type = resolvedType.get();

        TelegramWarNotification notification = telegramWarNotificationRepository
                .findByWarAndType(war, type)
                .orElse(null);

        if (notification != null && notification.getSentAt() != null) {
            return;
        }

        long targetChatId = resolveGroupChatId();
        if (targetChatId == 0) {
            log.debug("Telegram group is not configured, skipping war notification: warId={}", war.getId());
            return;
        }
        Integer targetThreadId = resolveWarThreadId(targetChatId);

        if (notification == null) {
            notification = telegramWarNotificationRepository.save(
                    new TelegramWarNotification(
                            war,
                            type,
                            targetChatId,
                            targetThreadId,
                            getWarNotificationMessage(war, type)));
        } else if (!Objects.equals(notification.getChatId(), targetChatId)
                || !Objects.equals(notification.getThreadId(), targetThreadId)) {
            notification.changeDestination(targetChatId, targetThreadId);
            notification = telegramWarNotificationRepository.save(notification);
        }

        telegramClient.sendMessage(
                notification.getChatId(),
                notification.getThreadId(),
                notification.getText());

        notification.markSent();
        telegramWarNotificationRepository.save(notification);

        log.info("War notification sent successfully: warId={}, type={}",
                war.getId(), type);
    }

    private String getWarNotificationMessage(@NonNull War war, WarNotificationType type) {
        String opponentClanName = TelegramUtils.escapeHtml(war.getOpponentClanName());
        String message = switch (type) {
            case PREPARATION -> "ПОДГОТОВКА";
            case FIRST_ATTACK -> "ВОЙНА НАЧАЛАСЬ. 1-я атака";
            case SECOND_ATTACK -> "12 ЧАСОВ. 2-я атака";
        };
        return message + " | ⚔ " + opponentClanName;
    }

    public Optional<WarNotificationType> resolveNotificationType(@NonNull War war) {
        Instant now = Instant.now();
        if ("warEnded".equals(war.getState()) || !now.isBefore(war.getEndTime())) {
            return Optional.empty();
        }
        if (now.isBefore(war.getStartTime())) {
            return Optional.of(WarNotificationType.PREPARATION);
        }
        Instant secondAttackTime = war.getStartTime().plus(Duration.ofHours(12));
        if (now.isBefore(secondAttackTime)) {
            return Optional.of(WarNotificationType.FIRST_ATTACK);
        }
        return Optional.of(WarNotificationType.SECOND_ATTACK);
    }

}
