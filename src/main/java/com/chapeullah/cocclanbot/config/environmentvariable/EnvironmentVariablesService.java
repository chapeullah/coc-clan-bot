package com.chapeullah.cocclanbot.config.environmentvariable;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.chapeullah.cocclanbot.config.environmentvariable.EnvironmentVariable.*;

@Component
@RequiredArgsConstructor
public class EnvironmentVariablesService {

    private final Environment environment;

    private static final Map<EnvironmentVariable, String> ENVIRONMENT_VARIABLES = Map.of(
            COC_API_BASE_URL, "COC_API_BASE_URL",
            COC_API_RESPONSE_TIMEOUT_MS, "COC_API_RESPONSE_TIMEOUT_MS",
            COC_API_TOKEN, "COC_API_TOKEN",
            COC_CLAN_TAG, "COC_CLAN_TAG",
            TELEGRAM_BOT_TOKEN, "TELEGRAM_BOT_TOKEN",
            TELEGRAM_SYSTEM_CHAT_ID, "TELEGRAM_SYSTEM_CHAT_ID",
            TELEGRAM_ADMIN_USER_IDS, "TELEGRAM_ADMIN_USER_IDS");

    @PostConstruct
    private void validate() {
        ENVIRONMENT_VARIABLES.values().forEach(this::requireValue);
        String timeoutVariable = ENVIRONMENT_VARIABLES.get(COC_API_RESPONSE_TIMEOUT_MS);
        Long timeoutMs = environment.getProperty(timeoutVariable, Long.class);
        if (timeoutMs == null || timeoutMs <= 0) {
            throw new IllegalStateException("Required environment variable must be greater than zero: " + timeoutVariable);
        }
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
