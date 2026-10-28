package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.rating.RatingService;
import com.chapeullah.cocclanbot.rating.WarRatingService;
import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.war.service.WarAttackService;
import com.chapeullah.cocclanbot.war.service.WarParticipantService;
import com.chapeullah.cocclanbot.war.service.WarService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotCommandWiringTest {

    @Test
    void allCommandBeansStartWithoutCircularDependenciesAndHelpFiltersAdministratorCommands() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("environmentVariablesService", Object.class, Object::new);
            for (Class<?> type : List.of(
                    ClanService.class, MemberService.class, WarService.class,
                    WarParticipantService.class, WarAttackService.class,
                    RaidSeasonService.class, RaidSeasonParticipantService.class,
                    RaidSeasonClanMemberSnapshotService.class, RatingService.class, WarRatingService.class,
                    TelegramGroupChatThreadsService.class, TelegramGroupChatIdService.class,
                    TelegramAccessService.class, CocProperties.class)) {
                registerMock(context, type);
            }
            context.scan("com.chapeullah.cocclanbot.telegram.command");
            context.refresh();

            TelegramMessage message = new TelegramMessage(1, 0,
                    new TelegramChat(7, "private", null, null, null, null),
                    new TelegramUser(7, null, null, null, false),
                    null, "/help", null, null, null, false);
            HelpCommand help = context.getBean(HelpCommand.class);
            String regularHelp = help.execute("", message);

            assertThat(regularHelp).contains("/clan —", "/member —", "/raid_missed —", BotMessages.USAGE_NOTICE)
                    .doesNotContain("/set_group —", "/reset_group —");
            var registeredCommands = context.getBean(BotCommandService.class).getCommandsByType().values().stream()
                    .flatMap(List::stream).toList();
            assertThat(registeredCommands).hasSize(context.getBeansOfType(BotCommand.class).size());

            when(context.getBean(TelegramAccessService.class).isAdmin(7L)).thenReturn(true);
            assertThat(help.execute("", message)).contains("/set_group —", "/reset_group —");
        }
    }

    private static <T> void registerMock(AnnotationConfigApplicationContext context, Class<T> type) {
        context.registerBean(type, () -> mock(type));
    }
}
