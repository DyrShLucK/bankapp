package com.frontservice.controller;

import com.frontservice.DTO.CashForm;
import com.frontservice.DTO.TransferForm;
import com.frontservice.service.SignupApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class TransferConroller {
    @Autowired
    SignupApi signupApi;

    @PostMapping("/transfer")
    public Mono<RedirectView> transfer(@ModelAttribute TransferForm form, WebSession session, ServerWebExchange exchange) {
        Object authAttribute = session.getAttributes().get("SPRING_SECURITY_CONTEXT");
        String username = null;
        if (authAttribute instanceof org.springframework.security.core.context.SecurityContextImpl) {
            var securityContext = (org.springframework.security.core.context.SecurityContextImpl) authAttribute;
            var authentication = securityContext.getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                var user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                username = user.getUsername();
            }
        }
        System.out.println(username);
        String currentUserLogin = exchange.getRequest().getHeaders().getFirst("X-User-Name");

        return signupApi.transfer(form, username).flatMap(dto -> {
            Map<String, Object> flashAttributes = new HashMap<>();
            if (form.getTo_login() != null && form.getTo_login().equals(currentUserLogin)) {
                flashAttributes.put("transferErrors", dto.getCause());
            } else {
                flashAttributes.put("transferOtherErrors", dto.getCause());
            }
            session.getAttributes().put("jakarta.servlet.flash.mapping.output", flashAttributes);
            RedirectView redirectView = new RedirectView("/bankapp", HttpStatusCode.valueOf(301));
            return Mono.just(redirectView);
        });
    }
}
