package com.notificationsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.notificationsRequests:notifications.requests}")
    private String notificationsRequestsTopic;

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final AtomicInteger IdNotification = new AtomicInteger(0);

    public Mono<NotificationsGet> getNotifications(String username) {
        logger.info("Getting notifications for user: {}", username);
        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());

        userNotifications.put(username, new CopyOnWriteArrayList<>());

        NotificationsGet response = new NotificationsGet();
        response.setNotifications(new ArrayList<>(currentNotifications));

        return Mono.just(response);
    }

    @KafkaListener(
            topics = "#{@environment.getProperty('kafka.topics.notificationsRequests', 'notifications.requests')}",
            groupId = "notifications-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotificationRequest(String notificationJson) {
        logger.info("Received notification JSON from Kafka: {}", notificationJson);

        try {
            Notification notification = objectMapper.readValue(notificationJson, Notification.class);

            notification.setId(IdNotification.getAndIncrement());

            String username = notification.getUsername();
            if (username == null || username.isEmpty()) {
                logger.warn("Received notification without username: {}", notification);
                return;
            }

            List<Notification> userNotificationList = userNotifications.computeIfAbsent(
                    username,
                    k -> new CopyOnWriteArrayList<>()
            );
            userNotificationList.add(notification);

            logger.info("Stored notification for user '{}'. Total notifications for user: {}", username, userNotificationList.size());

        } catch (JsonProcessingException e) {
            logger.error("Error deserializing notification JSON: {}", notificationJson, e);
            throw new RuntimeException("Failed to deserialize notification", e);
        } catch (Exception e) {
            logger.error("Unexpected error processing notification: {}", notificationJson, e);
            throw e;
        }
    }
}