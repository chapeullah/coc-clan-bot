package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.SetGroupCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SetGroupCommandTest {

    private static final long ADMIN_USER_ID = 12345L;
    private static final long GROUP_CHAT_ID = -1001234567890L;

    @Mock
    private TelegramGroupSettingRepository telegramGroupSettingRepository;

    @Spy
    private TelegramAccessService telegramAccessService = new TelegramAccessService();

    @InjectMocks
    private TelegramCommandService telegramCommandService;

    private TelegramCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        telegramAccessService.initializeAdminUserIds(Long.toString(ADMIN_USER_ID));
        dispatcher = new TelegramCommandDispatcher(List.of(new SetGroupCommand(telegramCommandService)));
    }

    @Test
    void savesTheGroupFromTheMessageWithoutRequiringArguments() {
        String response = dispatcher.execute("/set_group", "", message(GROUP_CHAT_ID, ADMIN_USER_ID));

        assertEquals("Эта группа назначена группой клана", response);
        ArgumentCaptor<TelegramGroupSetting> settings = ArgumentCaptor.forClass(TelegramGroupSetting.class);
        verify(telegramGroupSettingRepository).save(settings.capture());
        assertEquals(TelegramGroupSetting.SETTINGS_ID, settings.getValue().getId());
        assertEquals(GROUP_CHAT_ID, settings.getValue().getChatId());
    }

    @Test
    void rebindsTheSameSettingsRowToTheNewCurrentGroup() {
        dispatcher.execute("/set_group", "", message(GROUP_CHAT_ID, ADMIN_USER_ID));
        dispatcher.execute("/set_group", "", message(GROUP_CHAT_ID - 1, ADMIN_USER_ID));

        ArgumentCaptor<TelegramGroupSetting> settings = ArgumentCaptor.forClass(TelegramGroupSetting.class);
        verify(telegramGroupSettingRepository, times(2)).save(settings.capture());
        assertEquals(TelegramGroupSetting.SETTINGS_ID, settings.getAllValues().get(0).getId());
        assertEquals(TelegramGroupSetting.SETTINGS_ID, settings.getAllValues().get(1).getId());
        assertEquals(GROUP_CHAT_ID - 1, settings.getAllValues().get(1).getChatId());
    }

    @Test
    void doesNotUseAChatIdSuppliedAsAnArgument() {
        dispatcher.execute("/set_group", "-1009999999999", message(GROUP_CHAT_ID, ADMIN_USER_ID));

        ArgumentCaptor<TelegramGroupSetting> settings = ArgumentCaptor.forClass(TelegramGroupSetting.class);
        verify(telegramGroupSettingRepository).save(settings.capture());
        assertEquals(GROUP_CHAT_ID, settings.getValue().getChatId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {54321L})
    void rejectsUsersWhoAreNotBotAdmins(Long userId) {
        String response = dispatcher.execute("/set_group", "", message(GROUP_CHAT_ID, userId));

        assertEquals("Нет доступа", response);
        verifyNoInteractions(telegramGroupSettingRepository);
    }

    @Test
    void doesNotBindAPrivateChat() {
        String response = dispatcher.execute("/set_group", "", message(ADMIN_USER_ID, ADMIN_USER_ID));

        assertEquals("Вызови команду в нужной группе", response);
        verifyNoInteractions(telegramGroupSettingRepository);
    }

    private TelegramMessage message(long chatId, Long userId) {
        TelegramUser user = userId == null ? null : new TelegramUser(userId, "admin", "Admin", null);
        return new TelegramMessage(100L, 1790000000L, new TelegramChat(chatId), user, null, "/set_group");
    }
}
