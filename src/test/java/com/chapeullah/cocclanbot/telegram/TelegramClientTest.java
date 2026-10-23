package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramClientTest {

    private static final long GROUP_ID = -100123L;
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final String SENT_MESSAGE = """
            {"ok":true,"result":{"message_id":100,"date":1792746000,
            "chat":{"id":-100123,"type":"supergroup"},"text":"sent"}}
            """;

    private final JsonMapper json = JsonMapper.builder().build();
    private List<MockClientHttpRequest> requests;
    private TelegramOutgoingMessageService history;
    private TelegramClient client;
    private boolean rejectFirstReply;

    @BeforeEach
    void setUp() {
        requests = new ArrayList<>();
        history = mock(TelegramOutgoingMessageService.class);
        var restClient = RestClient.builder().baseUrl("https://telegram.test")
                .requestFactory((uri, method) -> {
                    var request = new MockClientHttpRequest(method, uri);
                    boolean reject = rejectFirstReply && requests.isEmpty();
                    String response = reject
                            ? "{\"ok\":false,\"description\":\"Bad Request: message to be replied not found\"}"
                            : SENT_MESSAGE;
                    request.setResponse(new MockClientHttpResponse(response.getBytes(StandardCharsets.UTF_8),
                            reject ? HttpStatus.BAD_REQUEST : HttpStatus.OK));
                    requests.add(request);
                    return request;
                }).build();
        client = new TelegramClient(restClient, json, history, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void ephemeralCommandReplyRemainsPrivate() {
        var source = message(11, true);
        client.sendReplyPart(source, "Members", TelegramOutgoingMessageContext.reply(1, source.telegramUser()));

        JsonNode body = requestBody(0);
        assertEquals(GROUP_ID, body.path("chat_id").asLong());
        assertEquals(11, body.path("message_thread_id").asInt());
        assertEquals(42L, body.path("ephemeral_message_parameters").path("receiver_user_id").asLong());
        assertEquals(77, body.path("reply_parameters").path("ephemeral_message_id").asInt());
        verify(history).startAttempt(any(), eq(GROUP_ID), eq(11), eq("Members"), eq("HTML"),
                eq(true), eq(77), anyString());
    }

    @Test
    void membersReplyRemainsPrivate() {
        var source = message(11, false);
        client.sendReplyPart(source, "Members", TelegramOutgoingMessageContext.reply(1, source.telegramUser()));

        JsonNode body = requestBody(0);
        assertEquals(42L, body.path("ephemeral_message_parameters").path("receiver_user_id").asLong());
        assertEquals(99L, body.path("reply_parameters").path("message_id").asLong());
    }

    @Test
    void unavailableReplyTargetDoesNotTurnAPrivateReplyIntoAPublicOne() {
        rejectFirstReply = true;
        var source = message(11, false);
        client.sendReplyPart(source, "Members", TelegramOutgoingMessageContext.reply(1, source.telegramUser()));

        assertEquals(2, requests.size());
        assertTrue(requestBody(0).has("reply_parameters"));
        assertFalse(requestBody(1).has("reply_parameters"));
        for (int index = 0; index < requests.size(); index++) {
            assertEquals(42L, requestBody(index).path("ephemeral_message_parameters")
                    .path("receiver_user_id").asLong());
            assertEquals(11, requestBody(index).path("message_thread_id").asInt());
        }
    }

    @Test
    void notificationsRemainPublic() {
        client.sendMessage(GROUP_ID, 11, "Notification");

        assertFalse(requestBody(0).has("ephemeral_message_parameters"));
        assertFalse(requestBody(0).has("reply_parameters"));
        assertEquals(11, requestBody(0).path("message_thread_id").asInt());
    }

    private JsonNode requestBody(int index) {
        return json.readTree(requests.get(index).getBodyAsString());
    }

    private static TelegramMessage message(Integer topic, boolean ephemeral) {
        return new TelegramMessage(ephemeral ? 0 : 99, NOW.getEpochSecond(),
                new TelegramChat(GROUP_ID, "supergroup", null, null, null, null),
                new TelegramUser(42L, null, "User", null, false), topic,
                "/members", ephemeral ? 77 : null,
                null, null, topic != null);
    }
}
