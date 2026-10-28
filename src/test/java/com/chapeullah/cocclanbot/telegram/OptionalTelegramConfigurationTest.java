package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.config.properties.TelegramProperties;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import com.chapeullah.cocclanbot.telegram.notification.TelegramWarNotificationRepository;
import com.chapeullah.cocclanbot.telegram.notification.TelegramWarNotificationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OptionalTelegramConfigurationTest {

    @Test
    void missingOptionalIdsDisableAdminAccessAndErrorNotifications() {
        TelegramProperties properties = new TelegramProperties();
        properties.setBotToken("test-token");
        properties.setBotPollDelayMs(1000L);
        properties.postConstruct();

        var groupService = mock(TelegramGroupChatIdService.class);
        var threadsService = mock(TelegramGroupChatThreadsService.class);
        var accessService = new TelegramAccessService(properties, groupService, threadsService);
        assertThat(accessService.isAdmin(7L)).isFalse();
        assertThat(accessService.isAdmin(null)).isFalse();

        TelegramClient client = mock(TelegramClient.class);
        var notificationService = new TelegramWarNotificationService(
                client, properties, mock(TelegramWarNotificationRepository.class),
                groupService, threadsService, mock(TelegramOutgoingMessageService.class));
        notificationService.sendError("test", new IllegalStateException("test"));

        verifyNoInteractions(client);
    }
}
