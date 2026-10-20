package com.chapeullah.guccigoblin.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TelegramAccessService {

    private Set<Long> adminUserIds = Set.of();

    @Value("${TELEGRAM_ADMIN_USER_IDS}")
    public void initializeAdminUserIds(String value) {
        adminUserIds = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(long userId) {
        return adminUserIds.contains(userId);
    }

}
