package com.chapeullah.guccigoblin.telegram.notification;

import com.chapeullah.guccigoblin.war.model.War;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramWarNotificationRepository extends JpaRepository<TelegramWarNotification, Long> {

    Optional<TelegramWarNotification> findByWarAndType(War war, WarNotificationType type);

}
