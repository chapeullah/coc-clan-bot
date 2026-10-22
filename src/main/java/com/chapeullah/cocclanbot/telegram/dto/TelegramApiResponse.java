package com.chapeullah.cocclanbot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramApiResponse<T>(
        boolean ok,
        T result,
        String description) {}
