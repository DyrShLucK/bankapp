package com.notificationsservice.controller;

import com.notification_service.api.DefaultApi;
import com.notification_service.domain.Notification;
import com.notification_service.domain.NotificationsGet;
import com.notificationsservice.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    ApiService apiService;

    @Override
    public Mono<ResponseEntity<NotificationsGet>> apiNotificationsGet(ServerWebExchange exchange) {
        return apiService.getNotifications(exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> apiNotificationsSetPost(Mono<Notification> notification, ServerWebExchange exchange) {
        return apiService.setNotification(notification).map(ResponseEntity::ok);
    }
}
