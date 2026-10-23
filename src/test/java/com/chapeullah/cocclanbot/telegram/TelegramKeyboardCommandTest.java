package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.SetKeyboardCommand;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
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

class TelegramKeyboardCommandTest {

    private static final long GROUP_ID = -100123L;
    private static final long ADMIN_ID = 42L;

    private TelegramGroupChatIdService group;
    private TelegramGroupChatThreadsService threads;
    private TelegramCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        group = mock(TelegramGroupChatIdService.class);
        threads = mock(TelegramGroupChatThreadsService.class);
        when(group.getChatId()).thenReturn(Optional.of(GROUP_ID));
        var access = new TelegramAccessService(Long.toString(ADMIN_ID), group, threads);
        var service = new TelegramCommandService(null, null, null, null, null, null,
                null, null, null, null, null, threads, group, access);
        dispatcher = new TelegramCommandDispatcher(List.of(new SetKeyboardCommand(service)), access);
    }

    @Test
    void administratorCanPublishTheKeyboardInSeveralTopicsAndGeneral() {
        for (Integer threadId : new Integer[]{11, 22, null}) {
            TelegramCommandReply reply = dispatcher.execute("/set_keyboard", "",
                    message(GROUP_ID, "supergroup", ADMIN_ID, threadId));

            assertTrue(reply.shared());
            assertEquals(List.of(List.of("/members")), reply.replyMarkup().get("keyboard"));
            assertEquals(true, reply.replyMarkup().get("is_persistent"));
            assertEquals(false, reply.replyMarkup().get("one_time_keyboard"));
            assertEquals(false, reply.replyMarkup().get("selective"));
        }
        verify(group, never()).upsertChatId(any());
        verify(threads, never()).upsertMainThreadId(any());
        verify(threads, never()).upsertWarThreadId(any());
        assertTrue(dispatcher.getCommands().getFirst().isAdminOnly());
    }

    @Test
    void ordinaryMemberCannotPublishTheKeyboard() {
        assertPrivateError(dispatcher.execute("/set_keyboard", "",
                message(GROUP_ID, "supergroup", 43L, 11)), "❌ Нет доступа");
    }

    @Test
    void administratorCannotPublishInAnotherGroup() {
        assertPrivateError(dispatcher.execute("/set_keyboard", "",
                message(-999L, "supergroup", ADMIN_ID, 11)),
                "❌ Команда доступна только в заданной группе.");
    }

    @Test
    void administratorCannotPublishInPrivateChat() {
        assertPrivateError(dispatcher.execute("/set_keyboard", "",
                message(ADMIN_ID, "private", ADMIN_ID, null)),
                "❌ Вызови команду в нужном чате группы");
    }

    @Test
    void configuredClanGroupIsRequired() {
        when(group.getChatId()).thenReturn(Optional.empty());

        assertPrivateError(dispatcher.execute("/set_keyboard", "",
                message(GROUP_ID, "supergroup", ADMIN_ID, 11)),
                "❌ Группа не задана: /set_group");
    }

    @Test
    void mainChatRestrictionAlsoAppliesToKeyboardSetup() {
        when(threads.isMainThread(11)).thenReturn(true);

        assertPrivateError(dispatcher.execute("/set_keyboard", "",
                message(GROUP_ID, "supergroup", ADMIN_ID, 11)),
                "❌ В этом чате команды бота запрещены. Используйте любой другой чат группы.");
    }

    @Test
    void invalidArgumentsDoNotPublishAKeyboard() {
        assertPrivateError(dispatcher.execute("/set_keyboard", "unexpected",
                message(GROUP_ID, "supergroup", ADMIN_ID, 11)), "❌ Используй: /set_keyboard");
    }

    private static void assertPrivateError(TelegramCommandReply reply, String text) {
        assertFalse(reply.shared());
        assertTrue(reply.replyMarkup().isEmpty());
        assertEquals(text, reply.text());
    }

    private static TelegramMessage message(long chatId, String type, long userId, Integer threadId) {
        return new TelegramMessage(0, 1792746000L,
                new TelegramChat(chatId, type, null, null, null, null),
                new TelegramUser(userId, null, "User", null, false),
                threadId, "/set_keyboard", 77, null, null, threadId != null);
    }
}
