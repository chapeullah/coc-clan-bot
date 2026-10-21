package com.chapeullah.guccigoblin.telegram;

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
}
