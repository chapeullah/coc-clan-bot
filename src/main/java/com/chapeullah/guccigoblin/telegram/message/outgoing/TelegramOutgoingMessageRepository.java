package com.chapeullah.guccigoblin.telegram.message.outgoing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TelegramOutgoingMessageRepository extends JpaRepository<TelegramOutgoingMessage, Long> {

    long countByDeliveryIdAndPartNumber(UUID deliveryId, int partNumber);
}
