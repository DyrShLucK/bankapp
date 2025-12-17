package com.frontservice.service;

import com.frontUi.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationDisplayService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDisplayService.class);

    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();

    public void sendNotificationToUser(String username, Notification notification) {
        List<Notification> list = userNotifications.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>());
        list.add(notification);
        logger.info("Added notification for user '{}'. Total notifications in cache: {}", username, list.size());
    }

    public List<Notification> getNotificationsForUser(String username) {
        List<Notification> list = userNotifications.getOrDefault(username, new CopyOnWriteArrayList<>());
        logger.info("Retrieved {} notifications for user '{}', clearing cache.", list.size(), username);

        userNotifications.put(username, new CopyOnWriteArrayList<>());
        return list;
    }
}