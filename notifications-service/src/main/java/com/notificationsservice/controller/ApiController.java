package com.notificationsservice.controller;

import com.notification_service.api.DefaultApi;
import com.notification_service.domain.NotificationsGet;
import com.notificationsservice.service.ApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    ApiService apiService;

    @Override
    public Mono<ResponseEntity<NotificationsGet>> apiNotificationsGet(@jakarta.annotation.Nullable String userName, ServerWebExchange exchange) {
        logger.info("Handling GET /api/notifications for user: {}", userName);

        if (userName == null) {
            logger.warn("GET /api/notifications called without username.");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return apiService.getNotifications(userName)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}