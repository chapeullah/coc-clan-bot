package com.chapeullah.cocclanbot.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties(prefix = "coc")
@Getter
@Slf4j
public class CocProperties {

    @Name("clan-tag") private String clanTag;

    @Name("api.token") private String apiToken;
    @Name("api.response-timeout-ms") private Long apiResponseTimeoutMs;
    @Name("api.base-url") private String apiBaseUrl;

    @PostConstruct
    public void postConstruct() {
        setClanTag(clanTag);
        setApiToken(apiToken);
        setApiResponseTimeoutMs(apiResponseTimeoutMs);
        setApiBaseUrl(apiBaseUrl);
    }

    public void setApiToken(String apiToken) {
        if (apiToken == null) {
            throw new IllegalArgumentException("coc.api-token must not be null");
        }
        if (apiToken.isBlank()) {
            throw new IllegalArgumentException("coc.api-token must not be blank");
        }
        this.apiToken = apiToken;
    }

    public void setApiResponseTimeoutMs(Long apiResponseTimeoutMs) {
        if (apiResponseTimeoutMs == null) {
            throw new IllegalArgumentException("coc.api-response-timeout-ms must not be null");
        }
        if (apiResponseTimeoutMs <= 0) {
            throw new IllegalArgumentException("coc.api-response-timeout-ms must be positive");
        }
        this.apiResponseTimeoutMs = apiResponseTimeoutMs;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null) {
            throw new IllegalArgumentException("coc.api.base-url must not be null");
        }
        if (apiBaseUrl.isBlank()) {
            throw new IllegalArgumentException("coc.api.base-url must not be blank");
        }
        this.apiBaseUrl = apiBaseUrl;
    }

    public void setClanTag(String clanTag) {
        if (clanTag == null) {
            throw new IllegalArgumentException("coc.clan-tag must not be null");
        }
        clanTag = clanTag.strip();
        if (clanTag.isBlank()) {
            throw new IllegalArgumentException("coc.clan-tag must not be blank");
        }
        if (!clanTag.matches("^#?[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException(
                    "coc.clan-tag must contain one or more Latin letters or digits, optionally prefixed with '#'");
        }
        if (!clanTag.startsWith("#")) {
            clanTag = "#" + clanTag;
        }
        this.clanTag = clanTag;
    }

}
