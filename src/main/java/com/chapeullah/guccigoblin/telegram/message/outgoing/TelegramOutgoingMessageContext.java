package com.chapeullah.guccigoblin.telegram.message.outgoing;

import com.chapeullah.guccigoblin.telegram.dto.TelegramUser;

import java.util.Objects;
import java.util.UUID;

public record TelegramOutgoingMessageContext(
        UUID deliveryId,
        TelegramOutgoingMessageType type,
        Long sourceUpdateId,
        Long notificationId,
        String operation,
        TelegramUser receiver,
        int partNumber,
        int partCount) {

    public TelegramOutgoingMessageContext {
        Objects.requireNonNull(deliveryId, "deliveryId");
        Objects.requireNonNull(type, "type");
        if (partNumber < 1 || partCount < partNumber) {
            throw new IllegalArgumentException("Invalid outgoing message part number or count");
        }
    }

    public static TelegramOutgoingMessageContext reply(long updateId, TelegramUser receiver) {
        return new TelegramOutgoingMessageContext(UUID.randomUUID(), TelegramOutgoingMessageType.COMMAND_REPLY,
                updateId, null, null, receiver, 1, 1);
    }

    public static TelegramOutgoingMessageContext warNotification(long notificationId) {
        return new TelegramOutgoingMessageContext(UUID.randomUUID(), TelegramOutgoingMessageType.WAR_NOTIFICATION,
                null, notificationId, null, null, 1, 1);
    }

    public static TelegramOutgoingMessageContext systemError(String operation) {
        return new TelegramOutgoingMessageContext(UUID.randomUUID(), TelegramOutgoingMessageType.SYSTEM_ERROR,
                null, null, operation, null, 1, 1);
    }

    public static TelegramOutgoingMessageContext message() {
        return new TelegramOutgoingMessageContext(UUID.randomUUID(), TelegramOutgoingMessageType.MESSAGE,
                null, null, null, null, 1, 1);
    }

    public TelegramOutgoingMessageContext part(int number, int count) {
        return new TelegramOutgoingMessageContext(deliveryId, type, sourceUpdateId, notificationId,
                operation, receiver, number, count);
    }
}
