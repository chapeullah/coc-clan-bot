package com.chapeullah.cocclanbot.telegram.message.incoming;

import com.chapeullah.cocclanbot.telegram.dto.TelegramUpdate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TelegramIncomingMessageService {

    private final TelegramIncomingMessageRepository telegramIncomingMessageRepository;

    @Transactional
    public void save(@NonNull TelegramUpdate update, @NonNull String rawJson) {
        var message = update.telegramMessage();

        if (message == null || message.telegramChat() == null) {
            return;
        }

        if (telegramIncomingMessageRepository.existsById(update.updateId())) {
            return;
        }

        var user = message.telegramUser();

        TelegramIncomingMessage telegramMessage = new TelegramIncomingMessage(
                update.updateId(),
                message.messageId(),
                message.telegramChat().id(),
                message.threadId(),
                user == null ? null : user.id(),
                user == null ? null : user.username(),
                user == null ? null : user.firstName(),
                user == null ? null : user.lastName(),
                message.text(),
                Instant.ofEpochSecond(message.date()),
                rawJson
        );

        telegramMessage.captureDetails(message);
        telegramIncomingMessageRepository.save(telegramMessage);
    }
}
