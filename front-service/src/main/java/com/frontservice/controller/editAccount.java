// src/main/java/com/frontservice/controller/editAccount.java
package com.frontservice.controller;

import com.frontUi.domain.AccountForm;
import com.frontUi.domain.MainPageResponse;
import com.frontUi.domain.UserForm;
import com.frontservice.DTO.PasswordUdateForm;
import com.frontservice.DTO.RegistrationForm;
import com.frontservice.DTO.UserUpdateForm;
import com.frontservice.service.SignupApi;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class editAccount {

    private static final Logger logger = LoggerFactory.getLogger(editAccount.class);

    @Autowired
    SignupApi signupApi;

    @PostMapping("/editUserAccounts")
    public Mono<RedirectView> editAccountsAndUser(Model model, @ModelAttribute UserUpdateForm form, WebSession session, ServerWebExchange exchange) {

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


        return signupApi.editAccountsAndUser(form, username)
                .flatMap(dto -> {
                    Map<String, Object> flashAttributes = new HashMap<>();
                    flashAttributes.put("userAccountsErrors", dto.getCause());
                    session.getAttributes().put("jakarta.servlet.flash.mapping.output", flashAttributes);
                    RedirectView redirectView = new RedirectView("/bankapp", HttpStatusCode.valueOf(301));
                    return Mono.just(redirectView);
                });
    }

    @PostMapping("/editPassword")
    public Mono<RedirectView> editPassword(Model model, @Valid @ModelAttribute PasswordUdateForm form, BindingResult result, WebSession session, ServerWebExchange exchange) {
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


        Map<String, Object> flashAttributes = new HashMap<>();
        if (result.hasErrors()) {
            flashAttributes.put("passwordErrors", result.getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.toList()));
            session.getAttributes().put("jakarta.servlet.flash.mapping.output", flashAttributes);
            return Mono.just(new RedirectView("/bankapp", HttpStatusCode.valueOf(301)));
        }

        return signupApi.editPassword(form, username).then(Mono.just(new RedirectView("/bankapp", HttpStatusCode.valueOf(301))));
    }
}