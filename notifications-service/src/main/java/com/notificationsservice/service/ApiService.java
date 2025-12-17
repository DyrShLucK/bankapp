package com.notificationsservice.service;

import com.notificationsservice.model.Notification;
//com.notification_service.domain.Notification;
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
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final AtomicLong idNotification = new AtomicLong(0);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<List<Notification>> getNotifications(String username) {
        logger.info("Getting notifications for user: {}", username);

        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());
        userNotifications.put(username, new CopyOnWriteArrayList<>());

        return Mono.just(new ArrayList<>(currentNotifications));
    }

    public void handleIncomingNotification(Notification notification) {
        String username = notification.getUsername();
        if (username == null || username.isEmpty()) {
            logger.warn("Username is null or empty, skipping notification.");
            return;
        }

        notification.setId(idNotification.getAndIncrement());

        logger.info("Sending notification to Kafka for user: {} with ID: {}", username, notification.getId());

        kafkaTemplate.send("notifications.requests", username, notification);
    }

    public void storeNotificationForUser(String username, Notification notification) {
        if (username == null || notification == null) {
            logger.warn("Attempted to store notification with null username or notification object.");
            return;
        }
        if (notification.getId() == null) {
            notification.setId(idNotification.getAndIncrement());
        }

        List<Notification> list = userNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        list.add(notification);
        logger.info("Stored notification for user '{}' from external source. Total notifications in cache: {}", username, list.size());
        handleIncomingNotification(notification);
    }

}