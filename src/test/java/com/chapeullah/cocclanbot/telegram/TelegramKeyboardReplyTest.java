package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.MembersCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramReceivedUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramKeyboardReplyTest {

    private static final long GROUP_ID = -100123L;
    private static final long KEYBOARD_ID = 100L;
    private static final int TOPIC_ID = 11;

    private TelegramClient client;
    private TelegramCommandService commands;
    private TelegramGroupChatThreadsService threads;
    private TelegramOutgoingMessageService history;
    private TelegramBot bot;

    @BeforeEach
    void setUp() {
        client = mock(TelegramClient.class);
        commands = mock(TelegramCommandService.class);
        threads = mock(TelegramGroupChatThreadsService.class);
        history = mock(TelegramOutgoingMessageService.class);
        var group = mock(TelegramGroupChatIdService.class);
        when(group.getChatId()).thenReturn(Optional.of(GROUP_ID));
        var access = new TelegramAccessService("42", group, threads);
        var dispatcher = new TelegramCommandDispatcher(List.of(new MembersCommand(commands)), access);
        bot = new TelegramBot(client, dispatcher, mock(TelegramIncomingMessageService.class), access, history);
        when(commands.getMembersMessage()).thenReturn("Members");
        when(client.splitMessage(anyString()))
                .thenAnswer(invocation -> List.of(invocation.getArgument(0, String.class)));
    }

    @ParameterizedTest
    @MethodSource("topics")
    void keyboardReplyRunsMembersInItsTopicWithAPrivateResponse(Integer topicId) {
        var source = message(GROUP_ID, topicId, "Участники клана", keyboardMessage(topicId, KEYBOARD_ID));
        when(history.hasSentSharedMessage(GROUP_ID, topicId, KEYBOARD_ID, "<b>Клавиатура бота</b>"))
                .thenReturn(true);

        poll(source);

        var context = ArgumentCaptor.forClass(TelegramOutgoingMessageContext.class);
        verify(commands).getMembersMessage();
        verify(client).sendReplyPart(eq(source), eq("Members"), context.capture(),
                eq(TelegramCommandReply.privateReply("Members")));
        assertEquals(source.telegramUser(), context.getValue().receiver());
    }

    @Test
    void ordinaryTextWithTheButtonLabelIsIgnored() {
        poll(message(GROUP_ID, TOPIC_ID, "Участники клана", null));

        verifyNoInteractions(commands, history);
        verify(client, never()).sendReplyPart(any(), anyString(), any(), any());
    }

    @Test
    void replyToAnUnrecognizedMessageWithTheSameTitleIsIgnored() {
        poll(message(GROUP_ID, TOPIC_ID, "Участники клана", keyboardMessage(TOPIC_ID, 999L)));

        verify(history).hasSentSharedMessage(GROUP_ID, TOPIC_ID, 999L, "<b>Клавиатура бота</b>");
        verifyNoInteractions(commands);
        verify(client, never()).sendReplyPart(any(), anyString(), any(), any());
    }

    @Test
    void messageFromAnotherTopicDoesNotActivateTheKeyboard() {
        when(history.hasSentSharedMessage(GROUP_ID, TOPIC_ID, KEYBOARD_ID, "<b>Клавиатура бота</b>"))
                .thenReturn(true);

        poll(message(GROUP_ID, 22, "Участники клана", keyboardMessage(TOPIC_ID, KEYBOARD_ID)));

        verify(history).hasSentSharedMessage(GROUP_ID, 22, KEYBOARD_ID, "<b>Клавиатура бота</b>");
        verifyNoInteractions(commands);
    }

    @Test
    void keyboardReplyStillRespectsTheMainChatRestriction() {
        when(history.hasSentSharedMessage(GROUP_ID, TOPIC_ID, KEYBOARD_ID, "<b>Клавиатура бота</b>"))
                .thenReturn(true);
        when(threads.isMainThread(TOPIC_ID)).thenReturn(true);
        var source = message(GROUP_ID, TOPIC_ID, "Участники клана", keyboardMessage(TOPIC_ID, KEYBOARD_ID));

        poll(source);

        verifyNoInteractions(commands);
        verify(client).sendReplyPart(eq(source), contains("В этом чате команды бота запрещены"), any(),
                argThat(reply -> !reply.shared()));
    }

    @Test
    void keyboardInAnOldClanGroupCannotBypassGroupAccessRules() {
        long previousGroup = -999L;
        when(history.hasSentSharedMessage(previousGroup, TOPIC_ID, KEYBOARD_ID, "<b>Клавиатура бота</b>"))
                .thenReturn(true);
        var source = message(previousGroup, TOPIC_ID, "Участники клана", keyboardMessage(TOPIC_ID, KEYBOARD_ID));

        poll(source);

        verifyNoInteractions(commands);
        verify(client).sendReplyPart(eq(source), contains("Бот работает только в группе клана"), any(),
                argThat(reply -> !reply.shared()));
    }

    @Test
    void unrelatedTextDoesNotQueryMessageHistory() {
        poll(message(GROUP_ID, TOPIC_ID, "Когда будут участники клана?", keyboardMessage(TOPIC_ID, KEYBOARD_ID)));

        verifyNoInteractions(commands, history);
    }

    private void poll(TelegramMessage source) {
        when(client.getUpdates(0)).thenReturn(
                List.of(new TelegramReceivedUpdate(new TelegramUpdate(123, source), "{}")));
        bot.poll();
    }

    private static Stream<Integer> topics() {
        return Stream.of(11, 22, null);
    }

    private static TelegramMessage message(long chatId, Integer topicId, String text, TelegramMessage reply) {
        return new TelegramMessage(101, 1792746000L,
                new TelegramChat(chatId, "supergroup", null, null, null, null),
                new TelegramUser(43L, null, "User", null, false),
                topicId, text, null, null, reply, topicId != null);
    }

    private static TelegramMessage keyboardMessage(Integer topicId, long messageId) {
        return new TelegramMessage(messageId, 1792745900L,
                new TelegramChat(GROUP_ID, "supergroup", null, null, null, null),
                new TelegramUser(555L, "clan_bot", "Bot", null, true),
                topicId, "Клавиатура бота", null, null, null, topicId != null);
    }
}
