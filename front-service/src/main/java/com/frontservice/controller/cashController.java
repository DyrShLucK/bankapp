package com.frontservice.controller;

import com.frontservice.DTO.CashForm;
import com.frontservice.service.SignupApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class cashController {
    @Autowired
    SignupApi signupApi;

    @PostMapping("/cash")
    public Mono<RedirectView> CashTransfer(Model model, @ModelAttribute CashForm form, WebSession session, ServerWebExchange exchange) {
        String sessionId = exchange.getRequest().getCookies().getFirst("SESSION").getValue();

        return signupApi.cash(form, sessionId)
                .flatMap(dto -> {
                    Map<String, Object> flashAttributes = new HashMap<>();
                    flashAttributes.put("cashErrors", dto.getCause());
                    session.getAttributes().put("jakarta.servlet.flash.mapping.output", flashAttributes);
                    RedirectView redirectView = new RedirectView("/bankapp", HttpStatusCode.valueOf(301));
                    return Mono.just(redirectView);
                });
    }
}
