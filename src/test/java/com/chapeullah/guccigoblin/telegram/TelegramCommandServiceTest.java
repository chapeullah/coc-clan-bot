package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.SetGroupCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.guccigoblin.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TelegramCommandServiceTest {

    private final TelegramGroupChatIdService groupService = mock(TelegramGroupChatIdService.class);
    private final TelegramGroupChatThreadsService threadsService = mock(TelegramGroupChatThreadsService.class);
    private final TelegramAccessService accessService = new TelegramAccessService("42", groupService);
    private final TelegramCommandService commandService = new TelegramCommandService(
            null, null, null, null, null, null, null, null, null, null,
            threadsService, groupService, accessService);

    @Test
    void privateHelpListsAllUserCommandsAndExplainsWhereTheyWork() {
        String help = commandService.help(message("private", 7, 7));

        assertThat(help).contains("/start —", "/help —", "/rating —", "/rating_cw —", "/rating_raids —",
                        "/clan —", "/members —", "/member #TAG", "/player #TAG", "/war —", "/wars —",
                        "/raid —", "/raid_missed —", "/raids —",
                        "В личных сообщениях работают только /start и /help.",
                        "Остальные команды используй в группе <a href=\"https://t.me/gussicoc\">ГУССИ</a>.",
                        "<i>В группе ваши команды и ответы бота видны только вам.</i>")
                .doesNotContain("/set_group", "/war_thread");
    }

    @Test
    void privateStartExplainsWhereToUseCommands() {
        assertThat(commandService.start(message("private", 7, 7)))
                .contains("В личных сообщениях работают только /start и /help.",
                        "Остальные команды используй в группе <a href=\"https://t.me/gussicoc\">ГУССИ</a>.",
                        "<i>В группе ваши команды и ответы бота видны только вам.</i>");
    }

    @Test
    void groupHelpListsUserCommandsWithoutAdministratorCommands() {
        assertThat(commandService.help(message("supergroup", -1001, 7)))
                .contains("/rating —", "/player #TAG", "/war —")
                .doesNotContain("/set_group", "/war_thread", "В личных сообщениях работают только", "видны только вам")
                .doesNotEndWith("\n");
        assertThat(commandService.start(message("supergroup", -1001, 7)))
                .doesNotContain("видны только вам")
                .doesNotEndWith("\n");
    }

    @Test
    void administratorKeepsFullHelpInPrivateChat() {
        assertThat(commandService.help(message("private", 42, 42)))
                .contains("/rating —", "/set_group", "/war_thread")
                .doesNotContain("В личных сообщениях работают только");
    }

    @Test
    void groupMembershipDoesNotAllowChangingSettings() {
        assertThat(commandService.setGroup(-1001, 7L)).isEqualTo("Нет доступа");
        assertThat(commandService.setWarThread(-1001, 7L, 10)).isEqualTo("Нет доступа");

        verifyNoInteractions(groupService, threadsService);
    }

    @Test
    void administratorCanConfigureFirstGroupThroughDispatcher() {
        when(groupService.getChatId()).thenReturn(Optional.empty());
        TelegramCommandDispatcher dispatcher = new TelegramCommandDispatcher(
                List.of(new SetGroupCommand(commandService)), accessService);

        assertThat(dispatcher.execute("/set_group", "", message("supergroup", -1001, 42)))
                .isEqualTo("Эта группа назначена группой клана");
        verify(groupService).upsertChatId(-1001L);
        verify(threadsService).resetThreadIds();
    }

    private TelegramMessage message(String type, long chatId, long userId) {
        return new TelegramMessage(1, 1, new TelegramChat(chatId, type, null, null, null, null),
                new TelegramUser(userId, null, "User", null, false), null, null, null, null, null);
    }
}
