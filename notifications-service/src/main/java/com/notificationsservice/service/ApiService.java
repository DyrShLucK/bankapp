package com.notificationsservice.service;

import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
import com.notificationsservice.model.KafkaNotificationRequest;
import com.notificationsservice.model.KafkaNotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
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
    private final AtomicInteger IdNotification = new AtomicInteger(0);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private NotificationsGet getNotificationsInternal(String username) {
        logger.info("Processing notification request for user: {}", username);
        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());

        userNotifications.put(username, new CopyOnWriteArrayList<>());

        NotificationsGet response = new NotificationsGet();
        response.setNotifications(new ArrayList<>(currentNotifications));
        logger.info("Retrieved {} notifications for user: {}", response.getNotifications().size(), username);
        return response;
    }

    public Mono<NotificationsGet> getNotifications(String username) {

        List<Notification> currentNotifications = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());

        userNotifications.put(username, new CopyOnWriteArrayList<>());

        NotificationsGet response = new NotificationsGet();
        response.setNotifications(new ArrayList<>(currentNotifications));

        return Mono.just(response);
    }

    public Mono<Void> setNotification(Mono<Notification> notificationMono) {
        return notificationMono.flatMap(n -> {
            String usernameFromNotification = n.getUsername();
            if (usernameFromNotification == null || usernameFromNotification.isEmpty()) {
                logger.warn("Received notification without username, ignoring: {}", n);
                return Mono.empty();
            }

            n.setId(IdNotification.getAndIncrement());

            List<Notification> userNotificationList = userNotifications.computeIfAbsent(
                    usernameFromNotification,
                    k -> new CopyOnWriteArrayList<>()
            );
            userNotificationList.add(n);
            logger.info("Stored notification ID {} for user: {}", n.getId(), usernameFromNotification);

            return Mono.empty(); // Возвращаем Mono<Void>
        });
    }
    // Kafka Listener для запросов на получение уведомлений
    @KafkaListener(topics = "notifications_requests", groupId = "notifications-service-group")
    public void handleNotificationRequest(KafkaNotificationRequest request, Acknowledgment acknowledgment) {
        try {
            logger.info("Received notification request for user: {}", request.getUsername());

            // Выполняем логику получения уведомлений
            NotificationsGet notificationsGet = getNotificationsInternal(request.getUsername());

            // Отправляем ответ в другой топик
            KafkaNotificationResponse response = new KafkaNotificationResponse(request.getUsername(), notificationsGet.getNotifications());
            kafkaTemplate.send("notifications_responses", request.getUsername(), response); // Используем username как ключ для партиционирования
            logger.info("Sent {} notifications to response topic for user: {}", response.getNotifications().size(), request.getUsername());

            // Подтверждаем обработку сообщения только после успешной отправки ответа
            acknowledgment.acknowledge();
            logger.debug("Acknowledged request message for user: {}", request.getUsername());
        } catch (Exception e) {
            logger.error("Error processing notification request for user: {}", request.getUsername(), e);
            // acknowledgment не вызывается, сообщение будет повторно обработано
            // В зависимости от логики, можно настроить Dead Letter Topic
        }
    }
}
