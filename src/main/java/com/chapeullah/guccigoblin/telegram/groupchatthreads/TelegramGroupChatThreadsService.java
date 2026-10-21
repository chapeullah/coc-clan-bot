package com.chapeullah.guccigoblin.telegram.groupchatthreads;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramGroupChatThreadsService {

    private final TelegramGroupChatThreadsRepository telegramGroupChatThreadsRepository;

    @Transactional
    public void resetThreadIds() {
        telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .ifPresent(threads -> threads.update(null, null));
    }

    @Transactional
    public void upsertThreadIds(Integer warThreadId, Integer raidThreadId) {
        TelegramGroupChatThreads telegramGroupChatThreads = telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .orElse(null);
        if (telegramGroupChatThreads == null) {
            telegramGroupChatThreads = new TelegramGroupChatThreads(warThreadId, raidThreadId);
        } else {
            telegramGroupChatThreads.update(warThreadId, raidThreadId);
        }
        telegramGroupChatThreadsRepository.save(telegramGroupChatThreads);
    }

    @Transactional
    public void upsertWarThreadId(Integer warThreadId) {
        TelegramGroupChatThreads telegramGroupChatThreads = telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .orElse(null);
        if (telegramGroupChatThreads == null) {
            telegramGroupChatThreads = new TelegramGroupChatThreads(warThreadId, null);
        } else {
            telegramGroupChatThreads.setWarThreadId(warThreadId);
        }
        telegramGroupChatThreadsRepository.save(telegramGroupChatThreads);
    }

    @Transactional
    public void upsertRaidThreadId(Integer raidThreadId) {
        TelegramGroupChatThreads telegramGroupChatThreads = telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .orElse(null);
        if (telegramGroupChatThreads == null) {
            telegramGroupChatThreads = new TelegramGroupChatThreads(null, raidThreadId);
        } else {
            telegramGroupChatThreads.setRaidThreadId(raidThreadId);
        }
        telegramGroupChatThreadsRepository.save(telegramGroupChatThreads);
    }

    @Transactional(readOnly = true)
    public Optional<Integer> getRaidThreadId() {
        return telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .map(TelegramGroupChatThreads::getRaidThreadId);
    }

    @Transactional(readOnly = true)
    public Optional<Integer> getWarThreadId() {
        return telegramGroupChatThreadsRepository
                .findById(TelegramGroupChatThreads.SETTING_ID)
                .map(TelegramGroupChatThreads::getWarThreadId);
    }

}
