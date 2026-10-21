package com.chapeullah.guccigoblin.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramChat(
        long id,
        String type,
        String title,
        String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName) {}
