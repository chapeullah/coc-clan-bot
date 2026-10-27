package com.chapeullah.cocclanbot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramMessage(
        @JsonProperty("message_id") long messageId,
        long date,
        @JsonProperty("chat") TelegramChat telegramChat,
        @JsonProperty("from") TelegramUser telegramUser,
        @JsonProperty("message_thread_id") Integer threadId,
        String text,
        @JsonProperty("ephemeral_message_id") Integer ephemeralMessageId,
        @JsonProperty("receiver_user") TelegramUser receiverUser,
        @JsonProperty("reply_to_message") TelegramMessage replyToMessage,
        @JsonProperty("is_topic_message") Boolean topicMessage) {

    public Integer forumThreadId() {
        return Boolean.TRUE.equals(topicMessage) ? threadId : null;
    }

}
