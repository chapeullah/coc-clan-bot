package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramBotTest {

    @Test
    void aMenuFailureDoesNotDiscardOrReexecuteTheCommand() {
        TelegramClient client = mock(TelegramClient.class);
        TelegramCommandDispatcher dispatcher = mock(TelegramCommandDispatcher.class);
        TelegramIncomingMessageService incoming = mock(TelegramIncomingMessageService.class);
        TelegramAccessService access = mock(TelegramAccessService.class);
        TelegramBot bot = new TelegramBot(client, dispatcher, incoming, access);
        var message = TelegramClientTest.message(77, null, null, false, Instant.now());
        var update = new TelegramUpdate(123, message);
        when(client.getUpdates(0)).thenReturn(List.of(new TelegramReceivedUpdate(update, "{}")));
        when(dispatcher.execute("/help", "", message)).thenReturn("answer");
        when(client.splitMessage("answer")).thenReturn(List.of("answer"));
        doThrow(new IllegalStateException("menu unavailable"))
                .when(client).configureChatCommands(anyList(), eq(message), anyBoolean());

        bot.poll();
        bot.poll();

        verify(dispatcher, times(1)).execute("/help", "", message);
        verify(client).sendReplyPart(eq(message), eq("answer"), any());
        verify(client).getUpdates(124);
    }
}
