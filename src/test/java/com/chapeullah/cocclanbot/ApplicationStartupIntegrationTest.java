package com.chapeullah.cocclanbot;

import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.clan.repository.ClanRepository;
import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.member.MemberRepository;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.command.BotCommandMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApplicationStartupIntegrationTest {

    @Test
    void applicationStartsWithCanonicalPropertiesAndMembersShowsOnlyCurrentRoster() {
        HttpClient httpClient = mock(HttpClient.class);
        HttpClient.Builder builder = mock(HttpClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(httpClient);

        // Use the real application beans and configuration, with no live HTTP transport or scheduled work.
        try (MockedStatic<HttpClient> clients = mockStatic(HttpClient.class)) {
            clients.when(HttpClient::newBuilder).thenReturn(builder);

            var environment = new StandardEnvironment();
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().addFirst(new MapPropertySource("startup-test", Map.ofEntries(
                    Map.entry("coc.clan-tag", "#STARTUP"),
                    Map.entry("coc.api.token", "startup-test-token"),
                    Map.entry("coc.api.base-url", "https://example.invalid/v1"),
                    Map.entry("coc.api.response-timeout-ms", "55000"),
                    Map.entry("telegram.bot.token", "startup-telegram-token"),
                    Map.entry("telegram.bot.poll-delay-ms", "1000"),
                    Map.entry("telegram.system-user-id", "7"),
                    Map.entry("telegram.admin-user-ids", "7,42"),
                    Map.entry("spring.datasource.url",
                            "jdbc:h2:mem:startup_check;MODE=PostgreSQL;NON_KEYWORDS=VALUE"),
                    Map.entry("spring.datasource.driver-class-name", "org.h2.Driver"),
                    Map.entry("spring.datasource.username", "sa"),
                    Map.entry("spring.datasource.password", ""),
                    Map.entry("spring.jpa.hibernate.ddl-auto", "create-drop"),
                    Map.entry("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", "true"),
                    Map.entry("app.readiness-file", "target/startup-test.ready"),
                    Map.entry("LOG_DIR", "target/startup-test-logs"),
                    Map.entry("logging.level.root", "ERROR"),
                    Map.entry("logging.level.com.chapeullah.cocclanbot", "ERROR"))));

            var application = new SpringApplication(Application.class);
            application.setEnvironment(environment);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setRegisterShutdownHook(false);
            application.setLogStartupInfo(false);
            application.addInitializers(context -> context.addBeanFactoryPostProcessor(beanFactory ->
                    ((DefaultListableBeanFactory) beanFactory).removeBeanDefinition(
                            TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)));

            try (var context = application.run()) {
                assertThat(context.containsBean("environmentVariablesService")).isFalse();
                assertThat(context.containsBean("scheduler")).isTrue();
                assertThat(context.containsBean("telegramBot")).isTrue();
                assertThat(context.getBean(CocProperties.class).getApiBaseUrl())
                        .isEqualTo("https://example.invalid/v1");
                assertThat(context.getBean(TelegramAccessService.class).isAdmin(42L)).isTrue();
                assertThat(context.getBean(TelegramAccessService.class).isAdmin(99L)).isFalse();

                var transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                transactions.executeWithoutResult(status -> {
                    Clan clan = new Clan("#STARTUP", "Startup", "open", null, null, null,
                            1, 0, 0, 0, null, 0, "always", 0, 0, 0, 0, true, null, 2, 0, 0, null, null);
                    context.getBean(ClanRepository.class).save(clan);
                    Member first = member(clan, "#FIRST", "First", 1);
                    Member second = member(clan, "#SECOND", "Second", 2);
                    Member former = member(clan, "#FORMER", "Former", 3);
                    former.leave(Instant.now());
                    MemberRepository repository = context.getBean(MemberRepository.class);
                    repository.saveAll(List.of(second, former, first));

                    assertThat(repository.findAllByClanTag("#STARTUP")).hasSize(3);
                    String message = context.getBean(BotCommandMessageService.class).getMembersMessage("#STARTUP");
                    assertThat(message).contains("<b>01. First</b>", "<b>02. Second</b>")
                            .doesNotContain("Former", "#FORMER");
                });

                assertThat(mockingDetails(httpClient).getInvocations())
                        .noneMatch(invocation -> invocation.getMethod().getName().startsWith("send"));
            }
        }
    }

    private static Member member(Clan clan, String tag, String name, int rank) {
        return new Member(tag, clan, name, "member", 10, 100,
                null, 1000, 0, rank, rank, 0, 0, null, null);
    }
}
