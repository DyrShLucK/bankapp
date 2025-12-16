package com.frontservice.service;

import com.frontUi.domain.Notification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificationDisplayService {

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();

    public void sendNotificationToUser(String username, Notification notification) {
        List<Notification> list = userNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        list.add(notification);
    }

    public List<Notification> getNotificationsForUser(String username) {
        List<Notification> list = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());
        userNotifications.put(username, new CopyOnWriteArrayList<>());
        return list;
    }
}