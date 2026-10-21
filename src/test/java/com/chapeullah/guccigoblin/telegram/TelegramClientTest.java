package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.command.BotCommand;
import com.chapeullah.guccigoblin.telegram.dto.TelegramChat;
import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.guccigoblin.telegram.message.outgoing.TelegramOutgoingMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramClientTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final TelegramOutgoingMessageService outgoingService = mock(TelegramOutgoingMessageService.class);
    private TelegramClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://telegram.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramClient(builder.build(), jsonMapper, outgoingService);
    }

    @Test
    void registersPrivateGroupCommandsAndOnlyStartAndHelpInPrivateMenu() {
        expectCommands("default", List.of("start", "help", "rating"));
        expectCommands("all_group_chats", List.of("start", "help", "rating"));
        expectCommands("all_private_chats", List.of("start", "help"));

        client.configureCommands(List.of(command("/start"), command("/help"), command("/rating")));

        server.verify();
    }

    @Test
    void rejectedRegistrationIsNotTreatedAsSuccess() {
        server.expect(requestTo("https://telegram.test/setMyCommands"))
                .andRespond(withSuccess("{\"ok\":false,\"description\":\"rejected\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.configureCommands(List.of(command("/start"))))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void groupReplyIsPrivateAndReferencesIncomingEphemeralCommand() {
        TelegramUser user = new TelegramUser(7, null, "User", null, false);
        TelegramMessage message = new TelegramMessage(0, 1,
                new TelegramChat(-1001, "supergroup", null, null, null, null),
                user, 10, "/help", 23, null, null);
        server.expect(requestTo("https://telegram.test/sendMessage"))
                .andExpect(request -> {
                    JsonNode body = readBody((MockClientHttpRequest) request);
                    assertThat(body.path("chat_id").asLong()).isEqualTo(-1001);
                    assertThat(body.path("message_thread_id").asInt()).isEqualTo(10);
                    assertThat(body.path("ephemeral_message_parameters").path("receiver_user_id").asLong())
                            .isEqualTo(7);
                    assertThat(body.path("reply_parameters").path("ephemeral_message_id").asInt()).isEqualTo(23);
                })
                .andRespond(withSuccess("""
                        {"ok":true,"result":{"message_id":0,"date":1,
                        "chat":{"id":-1001,"type":"supergroup"},"ephemeral_message_id":24}}
                        """, MediaType.APPLICATION_JSON));

        client.sendReplyPart(message, "help", TelegramOutgoingMessageContext.reply(1, user));

        server.verify();
    }

    @Test
    void privateReplyDoesNotUseGroupPrivacyParameters() {
        TelegramUser user = new TelegramUser(7, null, "User", null, false);
        TelegramMessage message = new TelegramMessage(1, 1,
                new TelegramChat(7, "private", null, null, null, null),
                user, null, "/help", null, null, null);
        expectRegularMessage(7);

        client.sendReplyPart(message, "help", TelegramOutgoingMessageContext.reply(1, user));

        server.verify();
    }

    @Test
    void groupNotificationsRemainVisibleToWholeGroup() {
        expectRegularMessage(-1001);

        client.sendMessage(-1001, 10, "War started");

        server.verify();
    }

    private void expectCommands(String scope, List<String> names) {
        server.expect(requestTo("https://telegram.test/setMyCommands"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    JsonNode body = readBody((MockClientHttpRequest) request);
                    assertThat(body.path("scope").path("type").asText()).isEqualTo(scope);
                    assertThat(body.path("commands").size()).isEqualTo(names.size());
                    for (int index = 0; index < names.size(); index++) {
                        JsonNode command = body.path("commands").get(index);
                        assertThat(command.path("command").asText()).isEqualTo(names.get(index));
                        assertThat(command.path("is_ephemeral").asBoolean()).isTrue();
                    }
                })
                .andRespond(withSuccess("{\"ok\":true,\"result\":true}", MediaType.APPLICATION_JSON));
    }

    private void expectRegularMessage(long chatId) {
        server.expect(requestTo("https://telegram.test/sendMessage"))
                .andExpect(request -> {
                    JsonNode body = readBody((MockClientHttpRequest) request);
                    assertThat(body.has("ephemeral_message_parameters")).isFalse();
                    assertThat(body.has("reply_parameters")).isFalse();
                })
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1,\"date\":1,"
                        + "\"chat\":{\"id\":" + chatId + ",\"type\":\"private\"}}}", MediaType.APPLICATION_JSON));
    }

    private JsonNode readBody(MockClientHttpRequest request) {
        return jsonMapper.readTree(request.getBodyAsString());
    }

    private BotCommand command(String name) {
        BotCommand command = mock(BotCommand.class);
        when(command.getName()).thenReturn(name);
        when(command.getDescription()).thenReturn(name + " description");
        return command;
    }
}
