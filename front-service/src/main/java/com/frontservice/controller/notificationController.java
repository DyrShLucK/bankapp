package com.frontservice.controller;

import com.frontUi.domain.Notification;
import com.frontUi.domain.NotificationsGet;
import com.frontservice.service.NotificationDisplayService;
import com.frontservice.service.SignupApi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class notificationController {

    private static final Logger logger = LoggerFactory.getLogger(notificationController.class);

    @Autowired
    private NotificationDisplayService displayService;

    @GetMapping("/notifications")
    public Mono<List<Notification>> notifications(@RequestParam("login") String login) {
        logger.info("Received request for notifications for user: {}", login);

        List<Notification> notifications = displayService.getNotificationsForUser(login);

        logger.info("Returning {} notifications for user: {}", notifications.size(), login);
        return Mono.justOrEmpty(notifications).defaultIfEmpty(List.of());
    }
}