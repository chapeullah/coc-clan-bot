package com.chapeullah.cocclanbot.telegram;

import lombok.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TelegramUtils {

    private static final Pattern GRAPHEME_PATTERN = Pattern.compile("\\X");
    private static final Pattern BOT_API_TOKEN_PATTERN =
            Pattern.compile("(?i)(https?://api\\.telegram\\.org/bot)[^/\\s\"?]+");

    private TelegramUtils() {}

    public static String maskBotToken(String value) {
        return value == null ? null : BOT_API_TOKEN_PATTERN.matcher(value).replaceAll("$1<redacted>");
    }

    public static String escapeHtml(@NonNull String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String replaceEmojisWithSquares(@NonNull String text) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = GRAPHEME_PATTERN.matcher(text);
        while (matcher.find()) {
            String symbol = matcher.group();
            boolean emoji = symbol.codePoints().anyMatch(codePoint ->
                    Character.isEmojiPresentation(codePoint)
                            || Character.isExtendedPictographic(codePoint)
                            || codePoint == 0x20E3);
            result.append(emoji ? "□" : symbol);
        }
        return result.toString();
    }

}
