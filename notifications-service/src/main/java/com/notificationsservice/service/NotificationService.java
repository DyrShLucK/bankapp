package com.notificationsservice.service;

import com.notificationsservice.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    // Хранилище уведомлений, ключ - username
    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0); // Для генерации ID уведомлений

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // --- Логика для получения уведомлений по запросу из Kafka ---
    private NotificationsGet getNotificationsInternal(String username) {
        logger.info("Processing notification request for user: {}", username);
        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());

        // Очищаем уведомления после получения (если это ваша логика)
        // ВАЖНО: Это может не подходить для всех сценариев, где пользователь может пропустить сообщение.
        // Рассмотрите альтернативы, например, хранение "прочитанных" уведомлений отдельно.
        userNotifications.put(username, new CopyOnWriteArrayList<>());

        NotificationsGet response = new NotificationsGet();
        response.setNotifications(new ArrayList<>(currentNotifications));
        logger.info("Retrieved {} notifications for user: {}", response.getNotifications().size(), username);
        return response;
    }

    // --- Kafka Listener для запросов на получение уведомлений ---
    @KafkaListener(topics = "notifications.requests", groupId = "notifications-service-group")
    public void handleNotificationRequest(KafkaNotificationRequest request, Acknowledgment acknowledgment) {
        try {
            logger.info("Received notification request from Kafka for user: {}", request.getUsername());

            // Выполняем логику получения уведомлений
            NotificationsGet notificationsGet = getNotificationsInternal(request.getUsername());

            // Отправляем ответ в топик Kafka
            KafkaNotificationResponse response = new KafkaNotificationResponse(request.getUsername(), notificationsGet.getNotifications());
            // Используем username как ключ для обеспечения, что все сообщения для одного пользователя идут в одну партицию (если важно)
            kafkaTemplate.send("notifications.responses", request.getUsername(), response);
            logger.info("Sent {} notifications to Kafka response topic for user: {}", response.getNotifications().size(), request.getUsername());

            // Подтверждаем обработку сообщения только после успешной отправки ответа
            acknowledgment.acknowledge();
            logger.debug("Acknowledged request message for user: {} from Kafka.", request.getUsername());
        } catch (Exception e) {
            logger.error("Error processing notification request for user: {} from Kafka", request.getUsername(), e);
            // acknowledgment не вызывается, сообщение будет повторно обработано в соответствии с настройками retry/max.poll.interval
            // В продвинутых сценариях может использоваться Dead Letter Topic
        }
    }

    // --- Логика для добавления уведомления (вызывается, например, через REST API или из других сервисов по Kafka) ---
    public Notification addNotification(String username, String message) {
        if (username == null || username.isEmpty()) {
            logger.warn("Attempted to add notification without username, ignoring message: {}", message);
            return null;
        }

        Notification notification = new Notification();
        notification.setId(idGenerator.getAndIncrement());
        notification.setUsername(username);
        notification.setMessage(message);
        notification.setTimestamp(Instant.now());

        List<Notification> userNotificationList = userNotifications.computeIfAbsent(
                username,
                k -> new CopyOnWriteArrayList<>()
        );
        userNotificationList.add(notification);

        logger.info("Stored notification ID {} for user: {}", notification.getId(), username);
        return notification;
    }

    // Если вы хотите принимать уведомления из другого Kafka топика (например, от других сервисов)
    // @KafkaListener(topics = "new.notifications.from.services", groupId = "notifications-service-group")
    // public void handleNewNotificationFromService(Notification notification, Acknowledgment acknowledgment) {
    //     try {
    //         addNotification(notification.getUsername(), notification.getMessage()); // или вся структура
    //         acknowledgment.acknowledge();
    //     } catch (Exception e) {
    //         logger.error("Error processing new notification from Kafka: {}", notification, e);
    //     }
    // }
}