package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.notification.TelegramNotificationService;
import com.chapeullah.guccigoblin.telegram.notification.TelegramWarNotification;
import com.chapeullah.guccigoblin.telegram.notification.TelegramWarNotificationRepository;
import com.chapeullah.guccigoblin.telegram.notification.WarNotificationType;
import com.chapeullah.guccigoblin.war.model.War;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    private static final long OLD_GROUP_CHAT_ID = -1001234567890L;
    private static final long SAVED_GROUP_CHAT_ID = -1009876543210L;
    private static final int OLD_THREAD_ID = 10;
    private static final int SAVED_THREAD_ID = 72;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private TelegramWarNotificationRepository notificationRepository;

    @Mock
    private TelegramChatSettingRepository chatSettingRepository;

    @Mock
    private TelegramGroupSettingRepository groupSettingRepository;

    @Mock
    private War war;

    private TelegramNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new TelegramNotificationService(
                telegramClient, notificationRepository, chatSettingRepository, groupSettingRepository);
        ReflectionTestUtils.setField(notificationService, "groupChatId", OLD_GROUP_CHAT_ID);
        ReflectionTestUtils.setField(notificationService, "warThreadId", OLD_THREAD_ID);
        when(war.getState()).thenReturn("inWar");
        when(war.getStartTime()).thenReturn(Instant.now().minusSeconds(60));
        when(war.getEndTime()).thenReturn(Instant.now().plusSeconds(3600));
    }

    @Test
    void sendsNewNotificationsToTheSavedGroupAndItsTopic() {
        when(groupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID))
                .thenReturn(Optional.of(new TelegramGroupSetting(SAVED_GROUP_CHAT_ID)));
        when(chatSettingRepository.findById(SAVED_GROUP_CHAT_ID))
                .thenReturn(Optional.of(new TelegramChatSetting(SAVED_GROUP_CHAT_ID, SAVED_THREAD_ID)));
        when(notificationRepository.save(any(TelegramWarNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(war.getOpponentClanName()).thenReturn("Opponent");

        notificationService.sendWarNotification(war);

        verify(telegramClient).sendMessage(SAVED_GROUP_CHAT_ID, SAVED_THREAD_ID,
                "ВОЙНА НАЧАЛАСЬ. 1-я атака | ⚔ Opponent");
    }

    @Test
    void movesAnUnsentNotificationToTheSavedGroupAndItsTopic() {
        TelegramWarNotification notification = new TelegramWarNotification(
                war, WarNotificationType.FIRST_ATTACK, OLD_GROUP_CHAT_ID, OLD_THREAD_ID, "Pending message");
        when(notificationRepository.findByWarAndType(war, WarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(notification));
        when(groupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID))
                .thenReturn(Optional.of(new TelegramGroupSetting(SAVED_GROUP_CHAT_ID)));
        when(chatSettingRepository.findById(SAVED_GROUP_CHAT_ID))
                .thenReturn(Optional.of(new TelegramChatSetting(SAVED_GROUP_CHAT_ID, SAVED_THREAD_ID)));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.sendWarNotification(war);

        assertEquals(SAVED_GROUP_CHAT_ID, notification.getChatId());
        assertEquals(SAVED_THREAD_ID, notification.getThreadId());
        assertNotNull(notification.getSentAt());
        verify(telegramClient).sendMessage(SAVED_GROUP_CHAT_ID, SAVED_THREAD_ID, "Pending message");
    }

    @Test
    void doesNotReuseAnotherGroupsTopicWhenTheGroupChanges() {
        when(groupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID))
                .thenReturn(Optional.of(new TelegramGroupSetting(SAVED_GROUP_CHAT_ID)));
        when(notificationRepository.save(any(TelegramWarNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(war.getOpponentClanName()).thenReturn("Opponent");

        notificationService.sendWarNotification(war);

        verify(telegramClient).sendMessage(SAVED_GROUP_CHAT_ID, null,
                "ВОЙНА НАЧАЛАСЬ. 1-я атака | ⚔ Opponent");
    }

    @Test
    void keepsTheEnvironmentDestinationUntilAGroupIsSaved() {
        when(notificationRepository.save(any(TelegramWarNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(war.getOpponentClanName()).thenReturn("Opponent");

        notificationService.sendWarNotification(war);

        verify(telegramClient).sendMessage(OLD_GROUP_CHAT_ID, OLD_THREAD_ID,
                "ВОЙНА НАЧАЛАСЬ. 1-я атака | ⚔ Opponent");
    }

    @Test
    void skipsNotificationsUntilAGroupIsConfigured() {
        ReflectionTestUtils.setField(notificationService, "groupChatId", 0L);

        notificationService.sendWarNotification(war);

        verifyNoInteractions(telegramClient);
        verify(notificationRepository, never()).save(any(TelegramWarNotification.class));
    }

    @Test
    void doesNotSendAnAlreadySentNotificationAgainAfterTheGroupChanges() {
        TelegramWarNotification notification = new TelegramWarNotification(
                war, WarNotificationType.FIRST_ATTACK, OLD_GROUP_CHAT_ID, OLD_THREAD_ID, "Sent message");
        notification.markSent();
        when(notificationRepository.findByWarAndType(war, WarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(notification));

        notificationService.sendWarNotification(war);

        verifyNoInteractions(telegramClient, groupSettingRepository, chatSettingRepository);
    }
}
