package com.chapeullah.cocclanbot.telegram.dto;

import java.util.Map;
import java.util.Objects;

public record TelegramCommandReply(String text, boolean shared, Map<String, Object> replyMarkup) {

    public TelegramCommandReply {
        Objects.requireNonNull(text, "text");
        replyMarkup = Map.copyOf(replyMarkup);
    }

    public static TelegramCommandReply privateReply(String text) {
        return new TelegramCommandReply(text, false, Map.of());
    }

    public static TelegramCommandReply sharedReply(String text, Map<String, Object> replyMarkup) {
        return new TelegramCommandReply(text, true, replyMarkup);
    }
}
