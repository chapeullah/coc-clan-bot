package com.chapeullah.cocclanbot.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "telegram")
@Getter
@Slf4j
public class TelegramProperties {

    @Name("bot.token") private String botToken;
    @Name("bot.poll-delay-ms") private Long botPollDelayMs;

    @Name("system-user-id") private Optional<Long> systemUserId = Optional.empty();
    @Name("admin-user-ids") private List<Long> adminUserIds = List.of();

    @PostConstruct
    public void postConstruct() {
        setBotToken(this.botToken);
        setBotPollDelayMs(this.botPollDelayMs);
        if (systemUserId.isEmpty()) {
            log.warn("No Telegram system user configured: telegram.system-user-id is empty; error notifications will be disabled");
        }
        if (adminUserIds.isEmpty()) {
            log.warn("No Telegram admins configured: telegram.admin-user-ids is empty; admin commands will be unavailable");
        }
    }

    public void setBotToken(String botToken) {
        if (botToken == null) {
            throw new IllegalArgumentException("telegram.bot.token must not be null");
        }
        if (botToken.isBlank()) {
            throw new IllegalArgumentException("telegram.bot.token must not be blank");
        }
        this.botToken = botToken;
    }

    public void setBotPollDelayMs(Long botPollDelayMs) {
        if (botPollDelayMs == null) {
            throw new IllegalArgumentException("telegram.bot.poll-delay-ms must not be null");
        }
        if (botPollDelayMs <= 0) {
            throw new IllegalArgumentException("telegram.bot.poll-delay-ms must be positive");
        }
        this.botPollDelayMs = botPollDelayMs;
    }

    public void setSystemUserId(Long systemUserId) {
        if (systemUserId == null) {
            throw new IllegalArgumentException("telegram.system-user-id must not be null");
        }
        if (systemUserId <= 0) {
            throw new IllegalArgumentException("telegram.system-user-id must be positive");
        }
        this.systemUserId = Optional.of(systemUserId);
    }

    public void setAdminUserIds(List<Long> adminUserIds) {
        if (adminUserIds == null) {
            throw new IllegalArgumentException("telegram.admin-user-ids must not be null");
        }
        if (adminUserIds.isEmpty()) {
            this.adminUserIds = List.of();
            return;
        }
        if (adminUserIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("telegram.admin-user-ids must contain only positive user IDs");
        }
        this.adminUserIds = List.copyOf(adminUserIds);
    }

}
