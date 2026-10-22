package com.chapeullah.cocclanbot.telegram.groupchatid;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramGroupChatIdService {

    private final TelegramGroupChatIdRepository telegramGroupChatIdRepository;

    @Transactional
    public void upsertChatId(Long chatId) {
        TelegramGroupChatId telegramGroupChatId = telegramGroupChatIdRepository
                .findById(TelegramGroupChatId.SETTING_ID)
                .orElse(null);
        if (telegramGroupChatId == null) {
            telegramGroupChatId = new TelegramGroupChatId(chatId);
        } else {
            telegramGroupChatId.setChatId(chatId);
        }
        telegramGroupChatIdRepository.save(telegramGroupChatId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getChatId() {
        return telegramGroupChatIdRepository
                .findById(TelegramGroupChatId.SETTING_ID)
                .map(TelegramGroupChatId::getChatId);
    }

}
