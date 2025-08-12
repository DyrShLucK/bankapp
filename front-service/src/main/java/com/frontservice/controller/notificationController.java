package com.frontservice.controller;

import com.frontUi.domain.Notification;
import com.frontUi.domain.NotificationsGet;
import com.frontservice.service.SignupApi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
public class notificationController {
//    @GetMapping("/notifications")
//    public Mono<List<Notification>> notifications() {
//        Notification notification = new Notification();
//        notification.setId(1L);
//        notification.setTimestamp(LocalDateTime.now());
//        notification.setMessage("Hello World");
//        notification.setUserId(12L);
//        return Mono.just(List.of(notification));
//    }
//    @Data
//    public class Notification {
//        private Long id;
//        private Long userId;
//        private String message;
//        private LocalDateTime timestamp;
//    }
    @Autowired
    SignupApi signupApi;
    @GetMapping("/notifications")
    public Mono<List<Notification>> notifications() {
        return signupApi.notification()
                .map(NotificationsGet::getNotifications)
                .defaultIfEmpty(new ArrayList<>());
    }
}
