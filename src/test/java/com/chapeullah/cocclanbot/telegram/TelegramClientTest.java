package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.command.*;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TelegramClientTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final TelegramOutgoingMessageService history = mock(TelegramOutgoingMessageService.class);
    private final List<JsonNode> requests = new ArrayList<>();
    private MockRestServiceServer server;
    private TelegramClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://telegram.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramClient(builder.build(), mapper, history, Clock.fixed(NOW, ZoneOffset.UTC));
        when(history.startAttempt(any(), anyLong(), nullable(Integer.class), anyString(), anyString(),
                anyBoolean(), nullable(Integer.class), anyString())).thenReturn(1L, 2L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bad Request: message to be replied not found", "Bad Request: REPLY_MESSAGE_ID_INVALID"})
    void aDeletedOrdinaryPrivateCommandDoesNotLoseItsAnswer(String description) {
        expectReplyError(description);
        expectSent(77);

        client.sendReplyPart(message(77, null, null, false, NOW), "answer",
                TelegramOutgoingMessageContext.message());

        server.verify();
        assertEquals(12, requests.getFirst().path("reply_parameters").path("message_id").asInt());
        assertFalse(requests.getLast().has("reply_parameters"));
        assertFalse(requests.getLast().has("ephemeral_message_parameters"));
        verify(history).markSent(eq(2L), any(), eq(200), anyString(), anyString());
    }

    @Test
    void aDeletedGroupCommandKeepsTheRecipientAndForumTopicOnRetry() {
        expectReplyError("Bad Request: message to be replied not found");
        expectSent(-100);

        client.sendReplyPart(message(-100, 42, null, true, NOW), "answer",
                TelegramOutgoingMessageContext.message());

        server.verify();
        for (JsonNode request : requests) {
            assertEquals(77, request.path("ephemeral_message_parameters").path("receiver_user_id").asLong());
            assertEquals(42, request.path("message_thread_id").asInt());
        }
        assertFalse(requests.getLast().has("reply_parameters"));
    }

    @Test
    void anExpiredEphemeralTargetStillFallsBackWithoutPublishingTheAnswer() {
        expectReplyError("Bad Request: REPLY_TO_INVALID");
        expectSent(-100);

        client.sendReplyPart(message(-100, 42, 99, true, NOW), "answer",
                TelegramOutgoingMessageContext.message());

        server.verify();
        assertEquals(99, requests.getFirst().path("reply_parameters").path("ephemeral_message_id").asInt());
        assertFalse(requests.getLast().has("reply_parameters"));
        assertEquals(77, requests.getLast().path("ephemeral_message_parameters").path("receiver_user_id").asLong());
    }

    @Test
    void anAlreadyExpiredEphemeralTargetIsNotSentAtAll() {
        expectSent(-100);

        client.sendReplyPart(message(-100, 42, 99, true, NOW.minusSeconds(15)), "answer",
                TelegramOutgoingMessageContext.message());

        server.verify();
        assertFalse(requests.getFirst().has("reply_parameters"));
        assertEquals(77, requests.getFirst().path("ephemeral_message_parameters").path("receiver_user_id").asLong());
    }

    @Test
    void anUnrelatedBadRequestIsNotRetried() {
        expectReplyError("Bad Request: can't parse entities");

        assertThrows(RestClientResponseException.class, () -> client.sendReplyPart(
                message(77, null, null, false, NOW), "answer", TelegramOutgoingMessageContext.message()));

        server.verify();
        assertEquals(1, requests.size());
    }

    @Test
    void sharedMenusNeverExposeAdministrativeCommands() {
        List<BotCommand> commands = commands();
        expectMenu();
        expectMenu();
        expectMenu();

        client.configureCommands(commands);

        server.verify();
        assertEquals(List.of("start", "help", "war"), menuNames(requests.get(0)));
        assertEquals(List.of("start", "help", "war"), menuNames(requests.get(1)));
        assertEquals(List.of("start", "help"), menuNames(requests.get(2)));
    }

    @Test
    void groupAdminMenusApplyOnlyToThatUserAndDoNotRepeatRequests() {
        expectMenu();
        expectMenu();
        TelegramMessage message = message(-100, 42, null, true, NOW);

        client.configureChatCommands(commands(), message, true);
        client.configureChatCommands(commands(), message, true);
        client.configureChatCommands(commands(), message, false);

        server.verify();
        JsonNode scope = requests.getFirst().path("scope");
        assertEquals("chat_member", scope.path("type").asString());
        assertEquals(-100, scope.path("chat_id").asLong());
        assertEquals(77, scope.path("user_id").asLong());
        assertTrue(menuNames(requests.getFirst()).contains("set_group"));
        assertFalse(menuNames(requests.getLast()).contains("set_group"));
    }

    @Test
    void privateMenusRespectTheSameAdminAccessRules() {
        expectMenu();
        expectMenu();
        TelegramMessage message = message(77, null, null, false, NOW);

        client.configureChatCommands(commands(), message, true);
        client.configureChatCommands(commands(), message, false);

        server.verify();
        assertEquals("chat", requests.getFirst().path("scope").path("type").asString());
        assertTrue(menuNames(requests.getFirst()).contains("set_group"));
        assertEquals(List.of("start", "help"), menuNames(requests.getLast()));
    }

    @Test
    void aFailedMenuUpdateCanBeRetried() {
        server.expect(requestTo("https://telegram.test/setMyCommands"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"temporary menu failure\"}"));
        expectMenu();
        TelegramMessage message = message(77, null, null, false, NOW);

        assertThrows(RestClientResponseException.class, () -> client.configureChatCommands(commands(), message, true));
        client.configureChatCommands(commands(), message, true);

        server.verify();
    }

    private List<BotCommand> commands() {
        return List.of(new StartCommand(null), new HelpCommand(null), new WarCommand(null),
                new SetGroupCommand(null), new SetWarThreadCommand(null), new SetMainThreadCommand(null),
                new ResetGroupCommand(null), new ResetWarThreadCommand(null), new ResetMainThreadCommand(null),
                new LinkMemberCommand(null), new UnlinkMemberCommand(null));
    }

    private List<String> menuNames(JsonNode request) {
        List<String> names = new ArrayList<>();
        for (JsonNode command : request.path("commands")) {
            names.add(command.path("command").asString());
            assertTrue(command.path("is_ephemeral").asBoolean());
        }
        return names;
    }

    private void expectReplyError(String description) {
        server.expect(requestTo("https://telegram.test/sendMessage"))
                .andExpect(request -> requests.add(mapper.readTree(((MockClientHttpRequest) request).getBodyAsString())))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.writeValueAsString(Map.of("ok", false, "description", description))));
    }

    private void expectSent(long chatId) {
        server.expect(requestTo("https://telegram.test/sendMessage"))
                .andExpect(request -> requests.add(mapper.readTree(((MockClientHttpRequest) request).getBodyAsString())))
                .andRespond(withSuccess(mapper.writeValueAsString(Map.of("ok", true, "result", Map.of(
                        "message_id", 500, "date", NOW.getEpochSecond(),
                        "chat", Map.of("id", chatId, "type", chatId > 0 ? "private" : "supergroup")))), MediaType.APPLICATION_JSON));
    }

    private void expectMenu() {
        server.expect(requestTo("https://telegram.test/setMyCommands"))
                .andExpect(request -> requests.add(mapper.readTree(((MockClientHttpRequest) request).getBodyAsString())))
                .andRespond(withSuccess("{\"ok\":true,\"result\":true}", MediaType.APPLICATION_JSON));
    }

    static TelegramMessage message(long chatId, Integer threadId, Integer ephemeralId, boolean topic, Instant date) {
        return new TelegramMessage(12, date.getEpochSecond(),
                new TelegramChat(chatId, chatId > 0 ? "private" : "supergroup", null, null, null, null),
                new TelegramUser(77, null, "User", null, false), threadId, "/help", ephemeralId, null, null, topic);
    }
}
