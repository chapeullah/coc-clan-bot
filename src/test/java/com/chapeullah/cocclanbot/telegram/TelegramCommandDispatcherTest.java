package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.HelpCommand;
import com.chapeullah.cocclanbot.telegram.command.MembersCommand;
import com.chapeullah.cocclanbot.telegram.command.StartCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelegramCommandDispatcherTest {

    private TelegramCommandService service;
    private TelegramCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        var group = mock(TelegramGroupChatIdService.class);
        when(group.getChatId()).thenReturn(Optional.of(-100123L));
        var access = new TelegramAccessService("42", group, mock(TelegramGroupChatThreadsService.class));
        service = mock(TelegramCommandService.class);
        dispatcher = new TelegramCommandDispatcher(List.of(
                new StartCommand(service), new HelpCommand(service), new MembersCommand(service)), access);
    }

    @Test
    void publicPrivateCommandsReachTheirHandlers() {
        var message = message(7, "private");
        when(service.start(message)).thenReturn("Start");
        when(service.help(message)).thenReturn("Help");

        assertEquals("Start", dispatcher.execute("/start", "", message));
        assertEquals("Help", dispatcher.execute("/help", "", message));
    }

    @Test
    void rejectedGroupCommandDoesNotClaimItWasSentInPrivate() {
        String response = dispatcher.execute("/members", "", message(-200, "supergroup"));

        assertEquals("❌ Команда доступна только в назначенной группе клана.", response);
        verifyNoInteractions(service);
    }

    @Test
    void rejectedPrivateCommandExplainsPrivateChatRestriction() {
        String response = dispatcher.execute("/members", "", message(42, "private"));

        assertTrue(response.contains("В личных сообщениях доступны только /start и /help"));
        verifyNoInteractions(service);
    }

    private static TelegramMessage message(long chatId, String type) {
        return new TelegramMessage(1, 1, new TelegramChat(chatId, type, null, null, null, null),
                new TelegramUser(chatId > 0 ? chatId : 42L, null, "User", null, false),
                null, "/members", null, null, null, false);
    }
}
