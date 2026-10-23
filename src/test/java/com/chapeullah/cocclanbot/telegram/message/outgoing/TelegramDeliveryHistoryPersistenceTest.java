package com.chapeullah.cocclanbot.telegram.message.outgoing;

import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelegramDeliveryHistoryPersistenceTest {

    private static AnnotationConfigApplicationContext context;
    private static TelegramOutgoingMessageRepository history;
    private static TransactionTemplate transaction;
    private static final Instant SENT_AT = Instant.parse("2026-10-23T10:00:00Z");

    @BeforeAll
    static void openDatabase() {
        context = new AnnotationConfigApplicationContext(JpaConfiguration.class);
        history = context.getBean(TelegramOutgoingMessageRepository.class);
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
        history.deleteAll();
    }

    @Test
    void onlyACompleteConfirmedWarNotificationCountsAsDeliveryEvidence() {
        transaction.executeWithoutResult(status -> {
            saveAttempt(5L, TelegramOutgoingMessageStatus.PENDING, 1, 1);
            saveAttempt(5L, TelegramOutgoingMessageStatus.FAILED, 1, 1);
            saveAttempt(5L, TelegramOutgoingMessageStatus.UNKNOWN, 1, 1);
            saveAttempt(5L, TelegramOutgoingMessageStatus.SENT, 1, 2);
            saveAttempt(6L, TelegramOutgoingMessageStatus.SENT, 1, 1);
            var unrelated = new TelegramOutgoingMessage(new TelegramOutgoingMessageContext(
                    java.util.UUID.randomUUID(), TelegramOutgoingMessageType.MESSAGE, null, 5L,
                    null, null, 1, 1), null, 1, -100L, 42, "unrelated", "HTML", false, null, "{}");
            unrelated.markSent(sentMessage(), 200, "{}", "{}");
            history.saveAndFlush(unrelated);
        });

        assertTrue(history.findWarNotificationSentAt(5L).isEmpty());
        assertEquals(SENT_AT, history.findWarNotificationSentAt(6L).orElseThrow());

        transaction.executeWithoutResult(status -> saveAttempt(5L, TelegramOutgoingMessageStatus.SENT, 1, 1));

        assertEquals(SENT_AT, history.findWarNotificationSentAt(5L).orElseThrow());
    }

    private void saveAttempt(long notificationId, TelegramOutgoingMessageStatus status, int part, int count) {
        var attempt = new TelegramOutgoingMessage(TelegramOutgoingMessageContext
                .warNotification(notificationId).part(part, count), null, 1, -100L, 42,
                "notification", "HTML", false, null, "{}");
        if (status == TelegramOutgoingMessageStatus.SENT) {
            attempt.markSent(sentMessage(), 200, "{}", "{}");
        } else if (status != TelegramOutgoingMessageStatus.PENDING) {
            attempt.markFailed(status, 500, "{}", "{}", "failure", "failure", null, null, null, null);
        }
        history.saveAndFlush(attempt);
    }

    private TelegramMessage sentMessage() {
        return new TelegramMessage(500, SENT_AT.getEpochSecond(),
                new TelegramChat(-100, "supergroup", null, null, null, null),
                null, 42, "notification", null, null, null, true);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = TelegramOutgoingMessageRepository.class)
    static class JpaConfiguration {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:telegram-history-regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.chapeullah.cocclanbot.telegram.message");
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
