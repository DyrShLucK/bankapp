// src/main/java/com/notificationsservice/kafka/KafkaConsumer.java
package com.notificationsservice.kafka;

import com.notification_service.domain.Notification;
import com.notificationsservice.service.ApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private ApiService apiService;

    // Используем топик bank-notifications, как требуется по заданию
    @KafkaListener(topics = "notifications.requests", groupId = "notification-group")
    public void consumeNotification(Notification notification) {
        logger.info("Consumed notification from Kafka: {}", notification);


        apiService.addNotificationToCache(notification);
    }
}