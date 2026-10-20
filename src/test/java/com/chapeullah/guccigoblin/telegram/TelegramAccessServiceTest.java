package com.chapeullah.guccigoblin.telegram;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramAccessServiceTest {

    @Test
    void initializesTheAdminSetBeforeInjectingTheServiceIntoItsConsumer() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("TELEGRAM_ADMIN_USER_IDS", " 12345, 9000000001, 12345, , ");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("test", properties));
            context.registerBean(AdminConsumer.class);
            context.registerBean(TelegramAccessService.class);
            context.refresh();

            assertTrue(context.getBean(AdminConsumer.class).adminAllowedAtConstruction);
            TelegramAccessService accessService = context.getBean(TelegramAccessService.class);
            assertTrue(accessService.isAdmin(9000000001L));
            assertFalse(accessService.isAdmin(12346L));

            properties.put("TELEGRAM_ADMIN_USER_IDS", "12346");

            assertTrue(accessService.isAdmin(12345L));
            assertTrue(accessService.isAdmin(9000000001L));
            assertFalse(accessService.isAdmin(12346L));
        }
    }

    static class AdminConsumer {

        private final boolean adminAllowedAtConstruction;

        AdminConsumer(TelegramAccessService accessService) {
            adminAllowedAtConstruction = accessService.isAdmin(12345L);
        }
    }
}
