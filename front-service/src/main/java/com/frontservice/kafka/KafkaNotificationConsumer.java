package com.frontservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "notifications.requests", groupId = "front-service-group")
    public void consumeNotification(Object rawMessage, @Header(KafkaHeaders.RECEIVED_KEY) String username) {
        try {
            Notification notification = objectMapper.convertValue(rawMessage, Notification.class);

            if (username == null) {
                username = notification.getUsername();
            }

            if (username == null) {
                logger.warn("Received notification without username (both key and payload), skipping: {}", notification);
                return;
            }

            logger.info("Received notification for user: {} with message: {}", username, notification.getMessage());
            displayService.sendNotificationToUser(username, notification);

        } catch (IllegalArgumentException | ClassCastException e) {
            logger.error("Failed to deserialize notification from Kafka: {}", rawMessage, e);
        }
    }
}