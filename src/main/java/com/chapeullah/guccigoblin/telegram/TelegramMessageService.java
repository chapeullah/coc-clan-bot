package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.telegram.dto.TelegramUpdate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final TelegramMessageRepository telegramMessageRepository;

    @Transactional
    public void save(@NonNull TelegramUpdate update, @NonNull String rawJson) {
        var message = update.telegramMessage();

        if (message == null || message.telegramChat() == null) {
            return;
        }

        if (telegramMessageRepository.existsById(update.updateId())) {
            return;
        }

        var user = message.telegramUser();

        TelegramMessage telegramMessage = new TelegramMessage(
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

        telegramMessageRepository.save(telegramMessage);
    }
}