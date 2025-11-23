package com.notificationsservice.model;



import java.util.List;

public class NotificationsGet {

    private List<Notification> notifications; // Согласно вашему OpenAPI, поле с маленькой 'n'

    // Конструктор по умолчанию
    public NotificationsGet() {
    }

    public NotificationsGet(List<Notification> notifications) {
        this.notifications = notifications;
    }

    // Getter and Setter
    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public String toString() {
        return "NotificationsGet{" +
                "notifications=" + notifications +
                '}';
    }
}