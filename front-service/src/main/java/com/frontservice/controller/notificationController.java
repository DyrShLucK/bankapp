package com.frontservice.controller;

import com.frontUi.domain.Notification;
import com.frontUi.domain.NotificationsGet;
import com.frontservice.service.SignupApi;
import lombok.AllArgsConstructor;
import lombok.Data;
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
    @Autowired
    SignupApi signupApi;

    @GetMapping("/notifications")
    public Mono<List<Notification>> notifications(
            @RequestParam("login") String login) {

        System.out.println("Входящий запрос: login = " + login);

        return signupApi.notification(login)
                .doOnNext(response -> System.out.println("Ответ от signupApi: " + response))
                .map(NotificationsGet::getNotifications)
                .doOnNext(notifications -> System.out.println("Возвращаемые уведомления: " + notifications))
                .defaultIfEmpty(new ArrayList<>());
    }
}
