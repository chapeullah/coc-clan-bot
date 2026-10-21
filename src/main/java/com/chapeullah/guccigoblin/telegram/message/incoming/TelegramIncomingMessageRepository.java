package com.chapeullah.guccigoblin.telegram.message.incoming;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramIncomingMessageRepository extends JpaRepository<TelegramIncomingMessage, Long> {}
