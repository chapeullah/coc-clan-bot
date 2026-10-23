package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageRepository;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessage;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageRepository;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TelegramKeyboardHistoryTest {

    private static final long GROUP_ID = -100123L;
    private static final String TITLE = "<b>Клавиатура бота</b>";
    private static LocalContainerEntityManagerFactoryBean factory;

    private EntityManager entityManager;
    private TelegramOutgoingMessageService history;

    @BeforeAll
    static void createDatabase() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(
                "jdbc:h2:mem:telegram_keyboard_history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        factory.setPackagesToScan("com.chapeullah.cocclanbot.telegram.message");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void closeDatabase() {
        if (factory != null) {
            factory.destroy();
        }
    }

    @BeforeEach
    void openSession() {
        entityManager = factory.getObject().createEntityManager();
        entityManager.getTransaction().begin();
        var repository = new JpaRepositoryFactory(entityManager).getRepository(TelegramOutgoingMessageRepository.class);
        history = new TelegramOutgoingMessageService(repository, mock(TelegramIncomingMessageRepository.class),
                JsonMapper.builder().build());
    }

    @AfterEach
    void closeSession() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @Test
    void matchesOnlyTheSentKeyboardMessageInTheSameChatAndTopic() {
        saveMessage(100, 11, TITLE, false, true);

        assertTrue(history.hasSentSharedMessage(GROUP_ID, 11, 100, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 101, TITLE));
        assertFalse(history.hasSentSharedMessage(-999L, 11, 100, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 22, 100, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 100, "Other message"));
    }

    @Test
    void generalTopicDoesNotMatchNamedTopics() {
        saveMessage(100, null, TITLE, false, true);
        saveMessage(101, 11, TITLE, false, true);

        assertTrue(history.hasSentSharedMessage(GROUP_ID, null, 100, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 100, TITLE));
        assertTrue(history.hasSentSharedMessage(GROUP_ID, 11, 101, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, null, 101, TITLE));
    }

    @Test
    void privatePendingAndUnrelatedMessagesAreNotKeyboardTargets() {
        saveMessage(100, 11, TITLE, true, true);
        saveMessage(101, 11, TITLE, false, false);
        saveMessage(102, 11, "Other message", false, true);

        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 100, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 101, TITLE));
        assertFalse(history.hasSentSharedMessage(GROUP_ID, 11, 102, TITLE));
    }

    @Test
    void committedKeyboardCanBeFoundInANewSession() {
        saveMessage(7777, 11, TITLE, false, true);
        entityManager.getTransaction().commit();
        entityManager.close();
        openSession();

        assertTrue(history.hasSentSharedMessage(GROUP_ID, 11, 7777, TITLE));
    }

    private void saveMessage(long messageId, Integer topicId, String text, boolean ephemeral, boolean sent) {
        var message = new TelegramOutgoingMessage(TelegramOutgoingMessageContext.message(),
                null, 1, GROUP_ID, topicId, text, "HTML", ephemeral, null, "{}");
        if (sent) {
            message.markSent(new TelegramMessage(messageId, 1792746000L,
                    new TelegramChat(GROUP_ID, "supergroup", null, null, null, null),
                    null, topicId, "Клавиатура бота", null, null, null, topicId != null),
                    200, "{}", "{}");
        }
        entityManager.persist(message);
    }
}
