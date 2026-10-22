package com.chapeullah.cocclanbot.telegram.notification;

import com.chapeullah.cocclanbot.war.model.War;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramWarNotificationRepository extends JpaRepository<TelegramWarNotification, Long> {

    Optional<TelegramWarNotification> findByWarAndType(War war, TelegramWarNotificationType type);

}
