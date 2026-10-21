package com.chapeullah.guccigoblin.telegram.groupchatthreads;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramGroupChatThreadsRepository
        extends JpaRepository<TelegramGroupChatThreads, Long> {}