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
    @Autowired
    SignupApi signupApi;
    @GetMapping("/notifications")
    public Mono<List<Notification>> notifications() {
        return signupApi.notification()
                .map(NotificationsGet::getNotifications)
                .defaultIfEmpty(new ArrayList<>());
    }
}
