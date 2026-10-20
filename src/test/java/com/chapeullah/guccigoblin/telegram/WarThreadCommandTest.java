package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.ChatIdCommand;
import com.chapeullah.guccigoblin.telegram.command.WarThreadCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarThreadCommandTest {

    private static final long FIRST_ADMIN_USER_ID = 12345L;
    private static final long SECOND_ADMIN_USER_ID = 9000000001L;
    private static final long GROUP_CHAT_ID = -1001234567890L;
    private static final int THREAD_ID = 72;
    private static final String SUCCESS_MESSAGE = "Эта тема назначена для уведомлений о КВ";

    @Mock
    private TelegramChatSettingRepository telegramChatSettingRepository;

    @Mock
    private TelegramGroupSettingRepository telegramGroupSettingRepository;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private TelegramMessageService telegramMessageService;

    @Spy
    private TelegramAccessService telegramAccessService = new TelegramAccessService();

    @InjectMocks
    private TelegramCommandService telegramCommandService;

    private TelegramCommandDispatcher dispatcher;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        telegramAccessService.initializeAdminUserIds(
                FIRST_ADMIN_USER_ID + ", " + SECOND_ADMIN_USER_ID + ", " + FIRST_ADMIN_USER_ID + ", ,");
        ReflectionTestUtils.setField(telegramCommandService, "groupChatId", GROUP_CHAT_ID);
        dispatcher = new TelegramCommandDispatcher(List.of(
                new WarThreadCommand(telegramCommandService),
                new ChatIdCommand(telegramCommandService)));
    }

    @Test
    void bindsTheTopicFromAnIncomingCommandAndRepliesInTheSameTopic() {
        TelegramUpdate update = jsonMapper.readValue("""
                {
                  "update_id": 41,
                  "message": {
                    "message_id": 100,
                    "date": 1790000000,
                    "from": {"id": 12345, "is_bot": false, "first_name": "Admin"},
                    "chat": {"id": -1001234567890, "type": "supergroup"},
                    "message_thread_id": 72,
                    "text": "/war_thread@gucci_test_bot"
                  }
                }
                """, TelegramUpdate.class);
        when(telegramClient.getUpdates(0L)).thenReturn(List.of(update));
        when(telegramClient.getUsername()).thenReturn("gucci_test_bot");
        when(telegramChatSettingRepository.findById(GROUP_CHAT_ID)).thenReturn(Optional.empty());

        new TelegramBot(telegramClient, dispatcher, telegramMessageService, jsonMapper).poll();

        ArgumentCaptor<TelegramChatSetting> setting = ArgumentCaptor.forClass(TelegramChatSetting.class);
        var order = inOrder(telegramMessageService, telegramChatSettingRepository);
        order.verify(telegramMessageService).save(update, jsonMapper.writeValueAsString(update));
        order.verify(telegramChatSettingRepository).save(setting.capture());
        assertEquals(GROUP_CHAT_ID, setting.getValue().getChatId());
        assertEquals(THREAD_ID, setting.getValue().getWarThreadId());
        verify(telegramClient).sendMessage(GROUP_CHAT_ID, THREAD_ID, SUCCESS_MESSAGE);
    }

    @Test
    void savesMessagesWithoutTextBeforeSkippingCommandHandling() {
        TelegramMessage message = new TelegramMessage(
                100L, 1790000000L, new TelegramChat(GROUP_CHAT_ID), null, THREAD_ID, null);
        TelegramUpdate update = new TelegramUpdate(41L, message);
        when(telegramClient.getUpdates(0L)).thenReturn(List.of(update));

        new TelegramBot(telegramClient, dispatcher, telegramMessageService, jsonMapper).poll();

        verify(telegramMessageService).save(update, jsonMapper.writeValueAsString(update));
        verify(telegramClient).getUpdates(0L);
        verifyNoMoreInteractions(telegramClient);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    @Test
    void retriesTheSameUpdateWithoutHandlingItWhenSavingFails() {
        TelegramUpdate update = new TelegramUpdate(
                41L, message(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID, THREAD_ID));
        when(telegramClient.getUpdates(0L)).thenReturn(List.of(update));
        doThrow(new IllegalStateException("Message history unavailable"))
                .when(telegramMessageService).save(update, jsonMapper.writeValueAsString(update));
        TelegramBot bot = new TelegramBot(telegramClient, dispatcher, telegramMessageService, jsonMapper);

        bot.poll();
        bot.poll();

        verify(telegramMessageService, times(2)).save(update, jsonMapper.writeValueAsString(update));
        verify(telegramClient, times(2)).getUpdates(0L);
        verifyNoMoreInteractions(telegramClient);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    @Test
    void changesTheExistingTopicForTheSameGroup() {
        TelegramChatSetting setting = new TelegramChatSetting(GROUP_CHAT_ID, 10);
        when(telegramChatSettingRepository.findById(GROUP_CHAT_ID)).thenReturn(Optional.of(setting));

        String response = dispatcher.execute("/war_thread", "", message(GROUP_CHAT_ID, SECOND_ADMIN_USER_ID, THREAD_ID));

        assertEquals(SUCCESS_MESSAGE, response);
        assertEquals(THREAD_ID, setting.getWarThreadId());
        verify(telegramChatSettingRepository).save(setting);
    }

    @Test
    void bindsTheTopicInTheSavedGroupInsteadOfTheEnvironmentGroup() {
        long savedGroupChatId = GROUP_CHAT_ID - 1;
        when(telegramGroupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID))
                .thenReturn(Optional.of(new TelegramGroupSetting(savedGroupChatId)));

        String response = dispatcher.execute("/war_thread", "",
                message(savedGroupChatId, FIRST_ADMIN_USER_ID, THREAD_ID));

        assertEquals(SUCCESS_MESSAGE, response);
        ArgumentCaptor<TelegramChatSetting> setting = ArgumentCaptor.forClass(TelegramChatSetting.class);
        verify(telegramChatSettingRepository).save(setting.capture());
        assertEquals(savedGroupChatId, setting.getValue().getChatId());
        assertEquals(THREAD_ID, setting.getValue().getWarThreadId());
    }

    @Test
    void rejectsTheOldGroupAfterAnotherGroupHasBeenSaved() {
        when(telegramGroupSettingRepository.findById(TelegramGroupSetting.SETTINGS_ID))
                .thenReturn(Optional.of(new TelegramGroupSetting(GROUP_CHAT_ID - 1)));

        String response = dispatcher.execute("/war_thread", "",
                message(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID, THREAD_ID));

        assertEquals("Команда доступна только в группе клана", response);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    @ParameterizedTest
    @MethodSource("rejectedCommands")
    void rejectsInvalidCommandsWithoutAccessingSettings(
            long chatId, Long userId, Integer threadId, String expectedResponse) {

        String response = dispatcher.execute("/war_thread", "", message(chatId, userId, threadId));

        assertEquals(expectedResponse, response);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    private static Stream<Arguments> rejectedCommands() {
        return Stream.of(
                Arguments.of(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID + 1, THREAD_ID, "Нет доступа"),
                Arguments.of(GROUP_CHAT_ID, null, THREAD_ID, "Нет доступа"),
                Arguments.of(GROUP_CHAT_ID - 1, FIRST_ADMIN_USER_ID, THREAD_ID, "Команда доступна только в группе клана"),
                Arguments.of(FIRST_ADMIN_USER_ID, FIRST_ADMIN_USER_ID, THREAD_ID, "Команда доступна только в группе клана"),
                Arguments.of(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID, null, "Отправь команду внутри нужной темы"),
                Arguments.of(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID, 0, "Отправь команду внутри нужной темы"),
                Arguments.of(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID, -1, "Отправь команду внутри нужной темы"));
    }

    @Test
    void ignoresAnAdminIdSuppliedAsAnArgumentByAnotherUser() {
        String response = dispatcher.execute("/war_thread", Long.toString(FIRST_ADMIN_USER_ID),
                message(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID + 1, THREAD_ID));

        assertEquals("Нет доступа", response);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    @Test
    void preservesTheExistingChatIdCommand() {
        String response = dispatcher.execute("/chat_id", "", message(GROUP_CHAT_ID, FIRST_ADMIN_USER_ID + 1, THREAD_ID));

        assertEquals("ID чата: <code>" + GROUP_CHAT_ID + "</code>", response);
        verifyNoInteractions(telegramChatSettingRepository);
    }

    private TelegramMessage message(long chatId, Long userId, Integer threadId) {
        TelegramUser user = userId == null ? null : new TelegramUser(userId, "admin", "Admin", null);
        return new TelegramMessage(100L, 1790000000L, new TelegramChat(chatId), user, threadId, "/war_thread");
    }
}
