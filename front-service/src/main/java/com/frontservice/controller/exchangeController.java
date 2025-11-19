package com.frontservice.controller;

import com.frontUi.domain.Value;
import com.frontservice.service.SignupApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class exchangeController {
    @Autowired
    SignupApi signupApi;
    @GetMapping("/exchange")
    public Mono<List<Value>> exchange(Model model) {
        return signupApi.getExchange().collectList();
    }
}
