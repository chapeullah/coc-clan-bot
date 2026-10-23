package com.chapeullah.cocclanbot.telegram.notification;

import com.chapeullah.cocclanbot.telegram.TelegramClient;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import com.chapeullah.cocclanbot.war.model.War;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramWarNotificationServiceTest {

    private final TelegramClient client = mock(TelegramClient.class);
    private final TelegramWarNotificationRepository notifications = mock(TelegramWarNotificationRepository.class);
    private final TelegramGroupChatIdService groups = mock(TelegramGroupChatIdService.class);
    private final TelegramGroupChatThreadsService threads = mock(TelegramGroupChatThreadsService.class);
    private final TelegramOutgoingMessageService history = mock(TelegramOutgoingMessageService.class);
    private War war;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        war = new War("#CLAN", "Clan", 0, 0, 0.0, 10,
                "#OTHER", "Opponent", 0, 0, 0.0, 10,
                "inWar", 20, 2, "none", now.minusSeconds(90000),
                now.minusSeconds(3600), now.plusSeconds(82800));
    }

    @Test
    void aFailedFinalSaveIsRecoveredFromDeliveryHistoryEvenAfterServiceRestart() {
        TelegramWarNotification first = pending();
        TelegramWarNotification reloaded = pending();
        Instant deliveredAt = Instant.now().minusSeconds(30);
        when(notifications.findByWarAndType(war, TelegramWarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(first)).thenReturn(Optional.of(reloaded));
        when(groups.getChatId()).thenReturn(Optional.of(-100L));
        when(threads.getWarThreadId()).thenReturn(Optional.of(42));
        when(history.findWarNotificationSentAt(5L))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(deliveredAt));
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new DataAccessResourceFailureException("commit failed"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(DataAccessResourceFailureException.class, () -> service().sendWarNotification(war));
        assertNull(reloaded.getSentAt());

        service().sendWarNotification(war);

        verify(client, times(1)).sendMessage(eq(-100L), eq(42), anyString(), any());
        assertEquals(deliveredAt, reloaded.getSentAt());
    }

    @Test
    void recoveryDoesNotRequireTheOldDestinationToStillBeConfigured() {
        TelegramWarNotification notification = pending();
        Instant deliveredAt = Instant.now().minusSeconds(30);
        when(notifications.findByWarAndType(war, TelegramWarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(notification));
        when(history.findWarNotificationSentAt(5L)).thenReturn(Optional.of(deliveredAt));

        service().sendWarNotification(war);

        assertEquals(deliveredAt, notification.getSentAt());
        verify(notifications).save(notification);
        verifyNoInteractions(client, groups, threads);
    }

    @Test
    void repeatedRecoverySaveFailuresNeverCauseAnotherSend() {
        when(notifications.findByWarAndType(war, TelegramWarNotificationType.FIRST_ATTACK))
                .thenAnswer(invocation -> Optional.of(pending()));
        when(history.findWarNotificationSentAt(5L)).thenReturn(Optional.of(Instant.now().minusSeconds(30)));
        when(notifications.save(any())).thenThrow(new DataAccessResourceFailureException("still unavailable"));

        assertThrows(DataAccessResourceFailureException.class, () -> service().sendWarNotification(war));
        assertThrows(DataAccessResourceFailureException.class, () -> service().sendWarNotification(war));

        verifyNoInteractions(client, groups, threads);
    }

    @Test
    void anUnconfirmedSendFailureIsNotMarkedAsDeliveredAndCanBeRetried() {
        TelegramWarNotification notification = pending();
        when(notifications.findByWarAndType(war, TelegramWarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(notification));
        when(history.findWarNotificationSentAt(5L)).thenReturn(Optional.empty());
        when(groups.getChatId()).thenReturn(Optional.of(-100L));
        when(threads.getWarThreadId()).thenReturn(Optional.of(42));
        when(notifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("send failed"))
                .doNothing().when(client).sendMessage(eq(-100L), eq(42), anyString(), any());

        assertThrows(IllegalStateException.class, () -> service().sendWarNotification(war));
        assertNull(notification.getSentAt());
        service().sendWarNotification(war);

        assertNotNull(notification.getSentAt());
        verify(client, times(2)).sendMessage(eq(-100L), eq(42), anyString(), any());
    }

    @Test
    void historyLookupFailureDoesNotRiskResendingAnAlreadyDeliveredMessage() {
        when(notifications.findByWarAndType(war, TelegramWarNotificationType.FIRST_ATTACK))
                .thenReturn(Optional.of(pending()));
        when(history.findWarNotificationSentAt(5L))
                .thenThrow(new DataAccessResourceFailureException("history unavailable"));

        assertThrows(DataAccessResourceFailureException.class, () -> service().sendWarNotification(war));

        verifyNoInteractions(client, groups, threads);
        verify(notifications, never()).save(any());
    }

    private TelegramWarNotification pending() {
        TelegramWarNotification notification = new TelegramWarNotification(
                war, TelegramWarNotificationType.FIRST_ATTACK, -100L, 42, "War started");
        ReflectionTestUtils.setField(notification, "id", 5L);
        return notification;
    }

    private TelegramWarNotificationService service() {
        return new TelegramWarNotificationService(client, notifications, groups, threads, history);
    }
}
