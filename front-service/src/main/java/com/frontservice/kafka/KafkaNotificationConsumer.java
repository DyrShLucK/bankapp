package com.frontservice.kafka;

import com.frontUi.domain.Notification;
import com.frontservice.service.NotificationDisplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    @Autowired
    private NotificationDisplayService displayService;

    @KafkaListener(topics = "notifications.requests", groupId = "front-service-group")
    public void consumeNotification(Notification notification, @Header(KafkaHeaders.RECEIVED_KEY) String username) {
        try {
            if (username == null) {
                username = notification.getUsername();
            }

            if (username == null) {
                logger.warn("Received notification without username (both key and payload), skipping: {}", notification);
                return;
            }

            displayService.sendNotificationToUser(username, notification);

        } catch (Exception e) {
            logger.error("Failed to process notification received from Kafka: {}", notification, e);
        }
    }
}