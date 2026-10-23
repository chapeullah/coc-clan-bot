package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.MembersCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramKeyboardPollingTest {

    @Test
    void retryPreservesSharedKeyboardAndDoesNotResendSuccessfulParts() {
        var client = mock(TelegramClient.class);
        var dispatcher = mock(TelegramCommandDispatcher.class);
        var incoming = mock(TelegramIncomingMessageService.class);
        var access = mock(TelegramAccessService.class);
        var bot = new TelegramBot(client, dispatcher, incoming, access);
        var message = message("/set_keyboard");
        var update = new TelegramUpdate(123, message);
        var reply = TelegramCommandReply.sharedReply("First\nSecond",
                Map.of("keyboard", List.of(List.of("/members"))));
        when(client.getUpdates(anyLong()))
                .thenReturn(List.of(new TelegramReceivedUpdate(update, "{}")))
                .thenReturn(List.of());
        when(dispatcher.execute("/set_keyboard", "", message)).thenReturn(reply);
        when(client.splitMessage(reply.text())).thenReturn(List.of("First", "Second"));
        doThrow(new RestClientResponseException("Retry later", 429, "Too Many Requests", null, null, null))
                .doNothing().when(client).sendReplyPart(eq(message), eq("Second"), any(), eq(reply));

        bot.poll();
        bot.poll();

        var contexts = ArgumentCaptor.forClass(TelegramOutgoingMessageContext.class);
        verify(client).sendReplyPart(eq(message), eq("First"), any(), eq(reply));
        verify(client, times(2)).sendReplyPart(eq(message), eq("Second"), contexts.capture(), eq(reply));
        assertEquals(contexts.getAllValues().get(0).deliveryId(), contexts.getAllValues().get(1).deliveryId());
        assertTrue(contexts.getAllValues().stream().allMatch(context -> context.receiver() == null));
        assertEquals(2, contexts.getValue().partNumber());
        verify(dispatcher).execute("/set_keyboard", "", message);
        verify(client).getUpdates(124);
    }

    @Test
    void membersButtonUsesTheExistingCommandPathAndPrivateReply() {
        var client = mock(TelegramClient.class);
        var service = mock(TelegramCommandService.class);
        var access = mock(TelegramAccessService.class);
        when(access.canExecute(eq("/members"), any())).thenReturn(true);
        when(service.getMembersMessage()).thenReturn("Members");
        var dispatcher = new TelegramCommandDispatcher(List.of(new MembersCommand(service)), access);
        var bot = new TelegramBot(client, dispatcher,
                mock(TelegramIncomingMessageService.class), access);
        var message = message("/members");
        var reply = TelegramCommandReply.privateReply("Members");
        when(client.getUpdates(0)).thenReturn(
                List.of(new TelegramReceivedUpdate(new TelegramUpdate(123, message), "{}")));
        when(client.splitMessage(reply.text())).thenReturn(List.of(reply.text()));

        bot.poll();

        var context = ArgumentCaptor.forClass(TelegramOutgoingMessageContext.class);
        verify(client).sendReplyPart(eq(message), eq("Members"), context.capture(), eq(reply));
        verify(service).getMembersMessage();
        assertEquals(message.telegramUser(), context.getValue().receiver());
    }

    private static TelegramMessage message(String text) {
        return new TelegramMessage(0, 1792746000L,
                new TelegramChat(-100123L, "supergroup", null, null, null, null),
                new TelegramUser(42L, null, "User", null, false),
                11, text, 77, null, null, true);
    }
}
