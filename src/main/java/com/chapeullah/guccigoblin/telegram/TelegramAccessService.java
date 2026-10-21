package com.chapeullah.guccigoblin.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@DependsOn("environmentVariablesService")
public class TelegramAccessService {

    private static final String ENVIRONMENT_VARIABLE = "TELEGRAM_ADMIN_USER_IDS";

    private final Set<Long> adminUserIds;

    public TelegramAccessService(@Value("${TELEGRAM_ADMIN_USER_IDS}") String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is missing or blank: " + ENVIRONMENT_VARIABLE);
        }
        try {
            this.adminUserIds = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Required environment variable must contain numeric user IDs: "
                    + ENVIRONMENT_VARIABLE, exception);
        }
        if (adminUserIds.isEmpty() || adminUserIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalStateException("Required environment variable must contain a nonempty list of positive user IDs: "
                    + ENVIRONMENT_VARIABLE);
        }
    }

    public boolean isAdmin(Long userId) {
        return userId != null && adminUserIds.contains(userId);
    }

}
