package com.chapeullah.guccigoblin.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramMessage(
        @JsonProperty("message_id") long messageId,
        long date,
        @JsonProperty("chat") TelegramChat telegramChat,
        @JsonProperty("from") TelegramUser telegramUser,
        @JsonProperty("message_thread_id") Integer threadId,
        String text) {}