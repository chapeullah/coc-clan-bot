package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelegramAccessServiceTest {

    private static final long ADMIN_ID = 42L;
    private static final long USER_ID = 7L;
    private static final long GROUP_ID = -100123L;

    private TelegramGroupChatIdService group;
    private TelegramGroupChatThreadsService threads;
    private TelegramAccessService access;

    @BeforeEach
    void setUp() {
        group = mock(TelegramGroupChatIdService.class);
        threads = mock(TelegramGroupChatThreadsService.class);
        when(group.getChatId()).thenReturn(Optional.of(GROUP_ID));
        access = new TelegramAccessService("42", group, threads);
    }

    @ParameterizedTest
    @CsvSource({"42,/start", "42,/help", "7,/start", "7,/help"})
    void publicCommandsWorkInPrivateChatsForEveryone(long userId, String command) {
        assertTrue(access.canExecute(command, message(userId, userId, "private", command)));
        verifyNoInteractions(threads);
    }

    @ParameterizedTest
    @CsvSource({"42,/members", "42,/set_group", "42,/reset_group", "7,/members", "7,/set_group"})
    void clanCommandsAreBlockedInPrivateChatsIncludingForAdmins(long userId, String command) {
        assertFalse(access.canExecute(command, message(userId, userId, "private", command)));
    }

    @ParameterizedTest
    @CsvSource({"42,group", "42,supergroup", "7,group", "7,supergroup"})
    void clanCommandsWorkInConfiguredGroupWithoutPlayerLinks(long userId, String type) {
        assertTrue(access.canExecute("/members", message(userId, GROUP_ID, type, "/members")));
    }

    @ParameterizedTest
    @ValueSource(longs = {ADMIN_ID, USER_ID})
    void clanCommandsAreBlockedInOtherGroupsIncludingForAdmins(long userId) {
        assertFalse(access.canExecute("/members", message(userId, -200L, "supergroup", "/members")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"group", "supergroup"})
    void adminCanConfigureFirstGroupAndReplaceIt(String type) {
        var message = message(ADMIN_ID, -200L, type, "/set_group");
        when(group.getChatId()).thenReturn(Optional.empty());
        assertTrue(access.canExecute("/set_group", message));

        when(group.getChatId()).thenReturn(Optional.of(GROUP_ID));
        assertTrue(access.canExecute("/set_group", message));
    }

    @Test
    void regularUserCannotConfigureFirstGroup() {
        when(group.getChatId()).thenReturn(Optional.empty());
        assertFalse(access.canExecute("/set_group", message(USER_ID, GROUP_ID, "supergroup", "/set_group")));
    }

    @Test
    void adminCannotConfigureChannelAsGroup() {
        assertFalse(access.canExecute("/set_group", message(ADMIN_ID, -200L, "channel", "/set_group")));
    }

    @Test
    void missingMessageChatOrSenderIsRejected() {
        assertFalse(access.canExecute("/start", null));
        assertFalse(access.canExecute("/start", new TelegramMessage(1, 1, null, null,
                null, "/start", null, null, null, false)));
        assertFalse(access.canExecute("/start", new TelegramMessage(1, 1,
                new TelegramChat(USER_ID, "private", null, null, null, null), null,
                null, "/start", null, null, null, false)));
    }

    @Test
    void mainThreadRestrictionAndAdminRecoveryCommandsArePreserved() {
        when(threads.isMainThread(null)).thenReturn(true);
        var adminMessage = message(ADMIN_ID, GROUP_ID, "supergroup", "/members");
        var userMessage = message(USER_ID, GROUP_ID, "supergroup", "/members");

        assertTrue(access.isCommandBlockedInMainThread("/members", adminMessage));
        assertTrue(access.isCommandBlockedInMainThread("/members", userMessage));
        assertFalse(access.isCommandBlockedInMainThread("/set_main_thread", adminMessage));
        assertFalse(access.isCommandBlockedInMainThread("/reset_main_thread", adminMessage));
        assertTrue(access.isCommandBlockedInMainThread("/reset_main_thread", userMessage));
    }

    private static TelegramMessage message(long userId, long chatId, String type, String command) {
        return new TelegramMessage(1, 1, new TelegramChat(chatId, type, null, null, null, null),
                new TelegramUser(userId, null, "User", null, false), null,
                command, null, null, null, false);
    }
}
