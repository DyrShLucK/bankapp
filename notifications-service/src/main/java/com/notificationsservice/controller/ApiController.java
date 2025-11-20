package com.notificationsservice.controller;

import com.notification_service.api.DefaultApi;
import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
import com.notificationsservice.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    ApiService apiService;
    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Override
    public Mono<ResponseEntity<NotificationsGet>> apiNotificationsGet(@jakarta.annotation.Nullable String SESSION, ServerWebExchange exchange) {


        if (SESSION == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return apiService.getNotifications(SESSION).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> apiNotificationsSetPost(Mono<Notification> notification, ServerWebExchange exchange) {
        return apiService.setNotification(notification).map(ResponseEntity::ok);
    }
}
