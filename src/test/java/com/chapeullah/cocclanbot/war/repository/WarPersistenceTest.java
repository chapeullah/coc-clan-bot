package com.chapeullah.cocclanbot.war.repository;

import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarLogEntry;
import com.chapeullah.cocclanbot.war.service.WarAttackService;
import com.chapeullah.cocclanbot.war.service.WarLogService;
import com.chapeullah.cocclanbot.war.service.WarParticipantService;
import com.chapeullah.cocclanbot.war.service.WarService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.chapeullah.cocclanbot.war.WarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WarPersistenceTest {

    private static AnnotationConfigApplicationContext context;
    private static WarRepository wars;
    private static WarLogRepository logs;
    private static WarAttackRepository attacks;
    private static TransactionTemplate transaction;

    @BeforeAll
    static void openDatabase() {
        context = new AnnotationConfigApplicationContext(JpaConfiguration.class);
        wars = context.getBean(WarRepository.class);
        logs = context.getBean(WarLogRepository.class);
        attacks = context.getBean(WarAttackRepository.class);
        transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterAll
    static void closeDatabase() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void clearDatabase() {
        transaction.executeWithoutResult(status -> {
            wars.deleteAll();
            logs.deleteAll();
        });
    }

    @Test
    void fullShiftedScheduleScenarioKeepsOneWarAndConfirmsTheFinalSummary() {
        Long originalId = transaction.execute(status -> {
            War existing = wars.saveAndFlush(war(ORIGINAL_START, ORIGINAL_END));
            wars.saveAndFlush(nextWar());
            logs.saveAndFlush(finalEntry(FINAL_END, 33, OPPONENT));
            return existing.getId();
        });
        ClashOfClansClient client = mock(ClashOfClansClient.class);
        when(client.getCurrentWar(CLAN))
                .thenReturn(response("warEnded", PREPARATION, SHIFTED_START, SHIFTED_END));
        WarService service = new WarService(client, wars, Clock.fixed(NOW, ZoneOffset.UTC),
                mock(WarParticipantService.class), mock(WarAttackService.class), new WarLogService(client, logs));

        transaction.executeWithoutResult(status -> service.syncCurrentWar(CLAN));
        transaction.executeWithoutResult(status -> assertEquals(1, service.syncEndedWarsFromWarLog(CLAN).size()));
        transaction.executeWithoutResult(status -> assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty()));
        transaction.executeWithoutResult(status -> service.syncCurrentWar(CLAN));

        transaction.executeWithoutResult(status -> {
            List<War> identity = wars.findAllByClanTagAndOpponentClanTagAndPreparationStartTimeOrderByIdAsc(
                    CLAN, OPPONENT, PREPARATION);
            assertEquals(1, identity.size());
            War confirmed = identity.getFirst();
            assertEquals(originalId, confirmed.getId());
            assertEquals(SHIFTED_START, confirmed.getStartTime());
            assertEquals(FINAL_END, confirmed.getEndTime());
            assertEquals(33, confirmed.getClanAttacks());
            assertEquals(60, confirmed.getClanStars());
            assertEquals(60, confirmed.getOpponentClanStars());
            assertEquals(NOW, confirmed.getWarLogSyncedAt());
            assertEquals(2, wars.count());
            assertEquals(0, attacks.count(), "Summary confirmation must not fabricate individual attacks");
        });
    }

    @Test
    void samePreparationCannotBeInsertedAgainWithAnotherStart() {
        transaction.executeWithoutResult(status -> wars.saveAndFlush(war(ORIGINAL_START, ORIGINAL_END)));

        assertThrows(DataIntegrityViolationException.class, () -> transaction.executeWithoutResult(
                status -> wars.saveAndFlush(war(SHIFTED_START, SHIFTED_END))));
        assertEquals(1, wars.count());
    }

    @Test
    void differentPreparationsCanHaveTheSameOriginallyReportedStart() {
        transaction.executeWithoutResult(status -> {
            wars.saveAndFlush(war(ORIGINAL_START, ORIGINAL_END));
            wars.saveAndFlush(new War(CLAN, "Clan", 0, 0, 0.0, 10,
                    OPPONENT, "Opponent", 0, 0, 0.0, 28,
                    "warEnded", 20, 2, "none", PREPARATION.plusSeconds(60), ORIGINAL_START, ORIGINAL_END));
        });

        assertEquals(2, wars.count());
    }

    @Test
    void candidateQueryHonorsBothTimeBoundsAndTheOpponent() {
        transaction.executeWithoutResult(status -> {
            logs.saveAllAndFlush(List.of(
                    finalEntry(SHIFTED_START, 33, OPPONENT),
                    finalEntry(FINAL_END, 33, OPPONENT),
                    finalEntry(NEXT_PREPARATION, 33, OPPONENT),
                    finalEntry(NEXT_PREPARATION.plusSeconds(1), 33, OPPONENT),
                    finalEntry(FINAL_END.plusSeconds(1), 33, "#OTHER")));
        });

        List<WarLogEntry> candidates = logs
                .findByClanTagAndOpponentClanTagAndEndTimeGreaterThanAndEndTimeLessThanEqualOrderByEndTimeAsc(
                        CLAN, OPPONENT, SHIFTED_START, NEXT_PREPARATION);

        assertEquals(List.of(FINAL_END, NEXT_PREPARATION),
                candidates.stream().map(WarLogEntry::getEndTime).toList());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = WarRepository.class)
    static class JpaConfiguration {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:war-regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.chapeullah.cocclanbot.war.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}
