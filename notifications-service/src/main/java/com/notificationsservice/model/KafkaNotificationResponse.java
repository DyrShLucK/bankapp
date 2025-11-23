package com.notificationsservice.model;

import java.util.List;

public class KafkaNotificationResponse {
    private String username;
    private List<Notification> notifications;

    public KafkaNotificationResponse() {}

    public KafkaNotificationResponse(String username, List<Notification> notifications) {
        this.username = username;
        this.notifications = notifications;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public String toString() {
        return "KafkaNotificationResponse{" +
                "username='" + username + '\'' +
                ", notifications=" + notifications +
                '}';
    }
}