package com.notificationsservice.service;

import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final AtomicInteger idNotification = new AtomicInteger(0);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<NotificationsGet> getNotifications(String username) {
        logger.info("Getting notifications for user: {}", username);

        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());

        // Очищаем список уведомлений для пользователя после получения
        userNotifications.put(username, new CopyOnWriteArrayList<>());

        NotificationsGet response = new NotificationsGet();
        response.setNotifications(new ArrayList<>(currentNotifications));

        return Mono.just(response);
    }

    public Mono<Void> handleIncomingNotification(Mono<Notification> notificationMono) {
        return notificationMono.flatMap(notification -> {
            String username = notification.getUsername();
            if (username == null || username.isEmpty()) {
                logger.warn("Username is null or empty, skipping notification.");
                return Mono.empty();
            }

            notification.setId(idNotification.getAndIncrement());

            logger.info("Sending notification to Kafka for user: {} with ID: {}", username, notification.getId());

            kafkaTemplate.send("notifications.requests", username, notification);

            return Mono.empty();
        });
    }


}