package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.telegram.dto.TelegramCommandReply;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import com.chapeullah.cocclanbot.telegram.message.incoming.TelegramIncomingMessageService;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageContext;
import com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot {

    private static final String COMMAND_ERROR_MESSAGE = "❌ Не удалось выполнить команду.";

    private final TelegramClient telegramClient;
    private final TelegramCommandDispatcher telegramCommandDispatcher;
    private final TelegramIncomingMessageService telegramIncomingMessageService;
    private final TelegramAccessService telegramAccessService;
    private final TelegramOutgoingMessageService telegramOutgoingMessageService;

    private long nextOffset;
    private PendingReply pendingReply;
    private boolean commandsConfigured;

    @Scheduled(fixedDelayString = "${TELEGRAM_BOT_POLL_DELAY_MS:1000}")
    public void poll() {
        if (telegramClient.isRateLimited()) {
            return;
        }
        log.debug("Telegram polling...");
        try {
            if (!commandsConfigured) {
                telegramClient.configureCommands(telegramCommandDispatcher.getCommands());
                commandsConfigured = true;
            }
            sendPendingReply();
            for (var receivedUpdate : telegramClient.getUpdates(nextOffset)) {
                TelegramUpdate update = receivedUpdate.update();
                telegramIncomingMessageService.save(update, receivedUpdate.rawJson());

                try {
                    handle(update);
                } catch (RestClientResponseException exception) {
                    int status = exception.getStatusCode().value();
                    if (!exception.getStatusCode().is4xxClientError() || status == 408 || status == 429) {
                        throw exception;
                    }
                    log.warn("Telegram update rejected, skipping: updateId={}, status={}",
                            update.updateId(), status, exception);
                }
                nextOffset = Math.max(nextOffset, update.updateId() + 1);
            }
            log.debug("Telegram polling success");
        } catch (RuntimeException exception) {
            log.warn("Telegram polling failed", exception);
        }
    }

    private void handle(TelegramUpdate telegramUpdate) {
        TelegramMessage telegramMessage = telegramUpdate.telegramMessage();
        if (telegramMessage == null || telegramMessage.telegramChat() == null || telegramMessage.text() == null) {
            return;
        }

        long chatId = telegramMessage.telegramChat().id();
        log.debug("Telegram IDs: chatId={}, threadId={}", chatId, telegramMessage.threadId());

        String messageString = resolveCommandText(telegramMessage);
        if (messageString == null) {
            return;
        }
        if (chatId < 0 && telegramMessage.telegramUser() == null) {
            log.warn("Cannot send a private command reply without sender: updateId={}", telegramUpdate.updateId());
            return;
        }

        String[] messageParts = messageString.split("\\s+", 2);
        String command = messageParts[0];
        String args = messageParts.length == 2 ? messageParts[1] : "";

        int usernameSeparator = command.indexOf('@');
        if (command.startsWith("/") && usernameSeparator > 0) {
            String username = command.substring(usernameSeparator + 1);
            if (!username.equalsIgnoreCase(telegramClient.getUsername())) {
                return;
            }
            command = command.substring(0, usernameSeparator);
        }

        try {
            Long userId = telegramMessage.telegramUser() == null ? null : telegramMessage.telegramUser().id();
            telegramClient.configureChatCommands(telegramCommandDispatcher.getCommands(), telegramMessage,
                    telegramAccessService.isAdmin(userId));
        } catch (RuntimeException exception) {
            // Menu configuration must not prevent execution or delivery of a command reply.
            log.warn("Failed to configure Telegram command menu: chatId={}", chatId, exception);
        }

        TelegramCommandReply response;
        try {
            response = telegramCommandDispatcher.execute(command, args, telegramMessage);
        } catch (RuntimeException exception) {
            log.error("Telegram command failed: updateId={}, command=\"{}\"",
                    telegramUpdate.updateId(), command, exception);
            response = TelegramCommandReply.privateReply(COMMAND_ERROR_MESSAGE);
        }

        List<String> parts;
        try {
            parts = telegramClient.splitMessage(response.text());
        } catch (IllegalArgumentException exception) {
            log.error("Telegram command response could not be formatted: updateId={}, command=\"{}\"",
                    telegramUpdate.updateId(), command, exception);
            parts = List.of(COMMAND_ERROR_MESSAGE);
            response = TelegramCommandReply.privateReply(COMMAND_ERROR_MESSAGE);
        }
        pendingReply = new PendingReply(telegramUpdate.updateId(), telegramMessage, parts, response);
        sendPendingReply();
    }

    private String resolveCommandText(TelegramMessage message) {
        String text = message.text().trim();
        if (text.startsWith("/")) {
            return text;
        }
        if (!TelegramCommandService.MEMBERS_BUTTON_TEXT.equals(text)) {
            return null;
        }
        TelegramMessage reply = message.replyToMessage();
        if (reply == null || reply.messageId() <= 0 || reply.ephemeralMessageId() != null) {
            return null;
        }
        return telegramOutgoingMessageService.hasSentSharedMessage(
                message.telegramChat().id(), message.forumThreadId(), reply.messageId(),
                TelegramCommandService.KEYBOARD_MESSAGE_TEXT) ? "/members" : null;
    }

    private void sendPendingReply() {
        if (pendingReply == null) {
            return;
        }
        try {
            while (pendingReply.nextPart < pendingReply.parts.size()) {
                telegramClient.sendReplyPart(pendingReply.message, pendingReply.parts.get(pendingReply.nextPart),
                        pendingReply.context.part(pendingReply.nextPart + 1, pendingReply.parts.size()),
                        pendingReply.reply);
                pendingReply.nextPart++;
            }
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (!exception.getStatusCode().is4xxClientError() || status == 408 || status == 429) {
                throw exception;
            }
            log.warn("Telegram reply rejected, skipping: updateId={}, status={}",
                    pendingReply.updateId, status, exception);
        }
        nextOffset = Math.max(nextOffset, pendingReply.updateId + 1);
        pendingReply = null;
    }

    private static final class PendingReply {
        private final long updateId;
        private final TelegramMessage message;
        private final List<String> parts;
        private final TelegramCommandReply reply;
        private final TelegramOutgoingMessageContext context;
        private int nextPart;

        private PendingReply(long updateId, TelegramMessage message, List<String> parts, TelegramCommandReply reply) {
            this.updateId = updateId;
            this.message = message;
            this.parts = parts;
            this.reply = reply;
            this.context = TelegramOutgoingMessageContext.reply(updateId, reply.shared() ? null : message.telegramUser());
        }
    }

}
