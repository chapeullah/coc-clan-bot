package com.chapeullah.guccigoblin.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnvironmentVariablesValidator {

    private final Environment environment;

    private static final List<String> ENVIRONMENT_VARIABLES = List.of(
            "COC_API_BASE_URL",
            "COC_API_TOKEN",
            "COC_CLAN_TAG",
            "TELEGRAM_BOT_TOKEN",
            "TELEGRAM_ADMIN_CHAT_ID",
            "TELEGRAM_ADMIN_USER_IDS",
            "TELEGRAM_CLAN_CHAT_LINK",
            "TELEGRAM_WAR_THREAD_ID",
            "TELEGRAM_RAID_THREAD_ID");

    @PostConstruct
    public void validate() {
        ENVIRONMENT_VARIABLES.forEach(this::requireValue);
    }

    private void requireValue(String name) {
        String value = environment.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Required environment variable is not set: " + name);
        }
        if (value.isBlank()) {
            throw new IllegalStateException("Required environment variable is blank: " + name);
        }
    }

}
