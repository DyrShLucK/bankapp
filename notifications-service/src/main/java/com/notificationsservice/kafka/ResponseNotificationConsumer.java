package com.notificationsservice.kafka;

import com.notificationsservice.model.Notification;
import com.notificationsservice.service.ApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ResponseNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ResponseNotificationConsumer.class);

    @Autowired
    private ApiService apiService;

    @KafkaListener(
            topics = "notifications.responses",
            groupId = "notification-service-responses-group",
            containerFactory = "responsesKafkaListenerContainerFactory"
    )
    public void consumeResponseNotification(Notification notification, @Header(KafkaHeaders.RECEIVED_KEY) String username) {
        try {
            if (username == null) {
                username = notification.getUsername();
            }

            if (username == null) {
                logger.warn("Received response notification without username (both key and payload), skipping: {}", notification);
                return;
            }

            logger.info("Received response notification from another service for user: {} with message: {}", username, notification.getMessage());
            
            apiService.storeNotificationForUser(username, notification); // Вызов метода, который ты реализуешь в ApiService

        } catch (Exception e) {
            logger.error("Failed to process response notification received from Kafka topic 'notifications.responses': {}", notification, e);
        }
    }
}