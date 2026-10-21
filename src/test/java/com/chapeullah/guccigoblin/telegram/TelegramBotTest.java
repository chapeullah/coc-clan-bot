package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.message.incoming.TelegramIncomingMessageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class TelegramBotTest {

    private final TelegramClient client = mock(TelegramClient.class);
    private final TelegramCommandDispatcher dispatcher = mock(TelegramCommandDispatcher.class);
    private final TelegramIncomingMessageService incomingService = mock(TelegramIncomingMessageService.class);
    private final TelegramBot bot = new TelegramBot(client, dispatcher, incomingService);

    @Test
    void configuresPrivacyOnceBeforeProcessingUpdates() {
        when(dispatcher.getCommands()).thenReturn(List.of());
        when(client.getUpdates(0)).thenReturn(List.of());

        bot.poll();
        bot.poll();

        var order = inOrder(client);
        order.verify(client).isRateLimited();
        order.verify(client).configureCommands(List.of());
        order.verify(client).getUpdates(0);
        verify(client).configureCommands(List.of());
        verify(client, times(2)).getUpdates(0);
    }

    @Test
    void retriesPrivacyConfigurationBeforeProcessingUpdatesAfterFailure() {
        when(dispatcher.getCommands()).thenReturn(List.of());
        doThrow(new IllegalStateException("configuration failed"))
                .doNothing().when(client).configureCommands(List.of());
        when(client.getUpdates(0)).thenReturn(List.of());

        bot.poll();
        verify(client, never()).getUpdates(anyLong());
        bot.poll();

        verify(client, times(2)).configureCommands(List.of());
        verify(client).getUpdates(0);
    }

    @Test
    void dispatchesStartAddressedToThisBotAndSendsReply() {
        TelegramMessage message = new TelegramMessage(0, 1,
                new TelegramChat(-1001, "supergroup", null, null, null, null),
                new TelegramUser(7, null, "User", null, false), 10, "/start@gussicoc_bot", 23, null, null);
        TelegramUpdate update = new TelegramUpdate(123, message);
        when(dispatcher.getCommands()).thenReturn(List.of());
        when(client.getUsername()).thenReturn("gussicoc_bot");
        when(client.getUpdates(0)).thenReturn(List.of(new TelegramReceivedUpdate(update, "{}")));
        when(dispatcher.execute("/start", "", message)).thenReturn("start");
        when(client.splitMessage("start")).thenReturn(List.of("start"));

        bot.poll();

        verify(dispatcher).execute("/start", "", message);
        verify(client).sendReplyPart(eq(message), eq("start"), any());
        when(client.getUpdates(124)).thenReturn(List.of());
        bot.poll();
        verify(client).getUpdates(124);
    }
}
