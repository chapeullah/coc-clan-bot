package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.groupchatid.TelegramGroupChatIdService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TelegramCommandDispatcherTest {

    private final TelegramGroupChatIdService groupService = mock(TelegramGroupChatIdService.class);
    private final TelegramAccessService accessService = new TelegramAccessService("42", groupService);

    @ParameterizedTest
    @CsvSource({
            "private,    7,    7, /start,  true",
            "private,    7,    7, /help,   true",
            "private,    7,    7, /rating, false",
            "private,    7,    7, /player, false",
            "private,    42,   42, /rating, true",
            "supergroup, -1001, 7, /rating, true",
            "group,      -1001, 7, /rating, true",
            "supergroup, -1002, 7, /rating, false",
            "supergroup, -1002, 7, /start,  false",
            "supergroup, -1002, 42, /rating, true",
            "channel,    -1001, 7, /rating, false"
    })
    void checksAccessBeforeExecutingCommand(String chatType, long chatId, long userId,
                                           String name, boolean allowed) {
        when(groupService.getChatId()).thenReturn(Optional.of(-1001L));
        BotCommand command = mock(BotCommand.class);
        when(command.getName()).thenReturn(name);
        TelegramMessage message = message(chatType, chatId, userId);
        when(command.execute("args", message)).thenReturn("executed");
        TelegramCommandDispatcher dispatcher = new TelegramCommandDispatcher(List.of(command), accessService);

        String response = dispatcher.execute(name, "args", message);

        if (allowed) {
            assertThat(response).isEqualTo("executed");
            verify(command).execute("args", message);
        } else {
            assertThat(response).contains("Бот работает только в группе клана", "/start", "/help");
            if ("private".equals(chatType)) {
                assertThat(response).contains("<i>В группе ваши команды и ответы бота видны только вам.</i>");
            } else {
                assertThat(response).doesNotContain("видны только вам");
            }
            verify(command, never()).execute(anyString(), any());
        }
    }

    @Test
    void deniesGroupCommandsUntilGroupIsConfigured() {
        when(groupService.getChatId()).thenReturn(Optional.empty());

        assertThat(accessService.canExecute("/rating", message("supergroup", -1001, 7))).isFalse();
        assertThat(accessService.canExecute("/start", message("private", 7, 7))).isTrue();
        assertThat(accessService.canExecute("/set_group", message("supergroup", -1001, 42))).isTrue();
    }

    @Test
    void appliesGroupChangesToSubsequentCommands() {
        when(groupService.getChatId()).thenReturn(Optional.of(-1001L)).thenReturn(Optional.of(-1002L));
        TelegramMessage oldGroup = message("supergroup", -1001, 7);

        assertThat(accessService.canExecute("/rating", oldGroup)).isTrue();
        assertThat(accessService.canExecute("/rating", oldGroup)).isFalse();
    }

    @Test
    void rejectsMissingContext() {
        assertThat(accessService.canExecute("/rating", null)).isFalse();
        TelegramMessage noChat = new TelegramMessage(1, 1, null, null, null, "/rating", null, null, null);
        assertThat(accessService.canExecute("/rating", noChat)).isFalse();
    }

    @Test
    void missingSenderDoesNotGrantAdministratorAccess() {
        TelegramMessage noSender = new TelegramMessage(1, 1,
                new TelegramChat(7, "private", null, null, null, null),
                null, null, "/rating", null, null, null);

        assertThat(accessService.canExecute("/rating", noSender)).isFalse();
    }

    private TelegramMessage message(String type, long chatId, long userId) {
        return new TelegramMessage(1, 1, new TelegramChat(chatId, type, null, null, null, null),
                new TelegramUser(userId, null, "User", null, false), null, null, null, null, null);
    }
}
