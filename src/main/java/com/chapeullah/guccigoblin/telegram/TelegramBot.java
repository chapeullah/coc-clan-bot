package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.dto.TelegramMessage;
import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot {

    private final TelegramClient telegramClient;
    private final TelegramCommandDispatcher telegramCommandDispatcher;

    private long nextOffset;

    @Scheduled(fixedDelayString = "${telegram.bot.poll-delay-ms:1000}")
    public void poll() {
        log.debug("Telegram polling...");
        try {
            for (var update : telegramClient.getUpdates(nextOffset)) {
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
        String messageString = telegramMessage.text().trim();
        if (messageString.isEmpty()) {
            telegramClient.sendMessage(chatId, "Неверный ввод.");
            return;
        }

        String[] messageParts = messageString.split("\\s+", 2);
        String command = messageParts[0];
        String args = messageParts.length == 2 ? messageParts[1] : "";

        telegramClient.sendMessage(
                chatId,
                telegramCommandDispatcher.execute(command, args, chatId));
    }

}
