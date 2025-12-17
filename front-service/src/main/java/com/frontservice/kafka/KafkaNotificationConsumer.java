package com.frontservice.kafka;

import com.frontUi.domain.Notification; // Используем класс из frontUi
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

    // Принимаем объект типа com.frontUi.domain.Notification напрямую
    @KafkaListener(topics = "notifications.requests", groupId = "front-service-group")
    public void consumeNotification(Notification notification, @Header(KafkaHeaders.RECEIVED_KEY) String username) {
        try {
            // ObjectMapper больше не нужен для конвертации

            if (username == null) {
                username = notification.getUsername(); // Получаем username из самого уведомления, если ключ Kafka пуст
            }

            if (username == null) {
                logger.warn("Received notification without username (both key and payload), skipping: {}", notification);
                return;
            }

            logger.info("Received notification for user: {} with message: {}", username, notification.getMessage());
            displayService.sendNotificationToUser(username, notification); // Передаем десериализованный объект

        } catch (Exception e) // Ловим любую ошибку десериализации/обработки
        {
            logger.error("Failed to process notification received from Kafka: {}", notification, e);
        }
    }
}