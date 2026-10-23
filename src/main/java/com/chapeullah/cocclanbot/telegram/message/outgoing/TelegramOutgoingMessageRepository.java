package com.chapeullah.cocclanbot.telegram.message.outgoing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TelegramOutgoingMessageRepository extends JpaRepository<TelegramOutgoingMessage, Long> {

    long countByDeliveryIdAndPartNumber(UUID deliveryId, int partNumber);

    @Query("""
            select count(message) > 0 from TelegramOutgoingMessage message
            where message.chatId = :chatId and message.messageId = :messageId
              and (message.threadId = :threadId or (message.threadId is null and :threadId is null))
              and message.status = com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageStatus.SENT
              and message.ephemeral = false and message.requestText = :requestText
            """)
    boolean existsSentSharedMessage(@Param("chatId") long chatId, @Param("threadId") Integer threadId,
                                   @Param("messageId") long messageId, @Param("requestText") String requestText);

    @Query("""
            select min(message.sentAt) from TelegramOutgoingMessage message
            where message.notificationId = :notificationId
              and message.type = com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageType.WAR_NOTIFICATION
              and message.status = com.chapeullah.cocclanbot.telegram.message.outgoing.TelegramOutgoingMessageStatus.SENT
              and message.partNumber = 1 and message.partCount = 1
            """)
    Optional<Instant> findWarNotificationSentAt(@Param("notificationId") Long notificationId);
}
