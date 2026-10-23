package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.SetWarThreadCommand;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetWarThreadCommandTest {

    @Mock private TelegramAccessService access;
    @Mock private TelegramGroupChatIdService groups;
    @Mock private TelegramGroupChatThreadsService threads;
    @InjectMocks private TelegramCommandService commands;

    @BeforeEach
    void setUp() {
        when(access.isAdmin(77L)).thenReturn(true);
        when(groups.getChatId()).thenReturn(Optional.of(-100L));
    }

    @Test
    void aNonForumThreadCannotBeSavedAsTheNotificationTopic() {
        var message = TelegramClientTest.message(-100, 999, null, false, Instant.now());

        String result = new SetWarThreadCommand(commands).execute("", message);

        assertTrue(result.startsWith("❌"));
        verifyNoInteractions(threads);
    }

    @Test
    void aRealForumTopicIsStillSavedForWarNotifications() {
        var message = TelegramClientTest.message(-100, 42, null, true, Instant.now());

        String result = new SetWarThreadCommand(commands).execute("", message);

        assertTrue(result.startsWith("✅"));
        verify(threads).upsertWarThreadId(42);
    }
}
