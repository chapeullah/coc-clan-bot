package com.chapeullah.guccigoblin.telegram.groupchatid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramGroupChatIdRepository
        extends JpaRepository<TelegramGroupChatId, Long> {}
