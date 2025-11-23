package com.frontservice.model;

public class KafkaNotificationRequest {
    private String username;

    public KafkaNotificationRequest() {}

    public KafkaNotificationRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "KafkaNotificationRequest{" +
                "username='" + username + '\'' +
                '}';
    }
}
