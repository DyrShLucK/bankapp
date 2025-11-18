package com.notificationsservice.service;

import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
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

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();

    private AtomicInteger IdNotification = new AtomicInteger(0);

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
                return Mono.empty();
            }


            n.setId(IdNotification.getAndIncrement());

            List<Notification> userNotificationList = userNotifications.computeIfAbsent(
                    usernameFromNotification,
                    k -> new CopyOnWriteArrayList<>()
            );
            userNotificationList.add(n);

            return Mono.empty();
        });
    }
}
