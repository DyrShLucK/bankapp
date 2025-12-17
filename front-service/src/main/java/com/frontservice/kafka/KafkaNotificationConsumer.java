package com.frontservice.kafka;

import com.frontUi.domain.Notification;
import com.frontservice.service.NotificationDisplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    @Autowired
    private NotificationDisplayService displayService;

    @KafkaListener(topics = "notifications.requests", groupId = "front-service-group")
    public void consumeNotification(Notification notification) {
        String username = notification.getUsername();
        if (username == null) {
            logger.warn("Received notification without username, skipping: {}", notification);
            return;
        }

        logger.info("Received notification from Kafka for user: {} with message: {}", username, notification.getMessage());

        // Отправляем уведомление в браузер
        displayService.sendNotificationToUser(username, notification);
    }
}