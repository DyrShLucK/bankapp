package com.frontservice.controller;

import com.frontUi.api.DefaultApi;
import com.frontUi.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
public class mainController {
    @Autowired
    DefaultApi defaultApi;

    @GetMapping({"/", "/bankapp"})
    public Mono<String> main(Model model, ServerWebExchange exchange, Principal principal, Authentication authentication, WebSession session) {



        return defaultApi.apiGetMainPageGet().flatMap(dto -> {
            Map<String, Object> flashAttributes = (Map<String, Object>) session.getAttributes().get("jakarta.servlet.flash.mapping.output");
            processAllFlashAttributes(model, flashAttributes, session);

            UserForm user = dto.getUser();
            List<AccountForm> accounts = dto.getAccounts();
            List<Currency> currency = dto.getCurrencys();
            List<Users> users = dto.getUsers();
            model.addAttribute("login", user.getLogin());
            model.addAttribute("name", user.getName());
            model.addAttribute("birthdate", user.getBirthdate().toString());
            model.addAttribute("accounts", accounts);
            model.addAttribute("currency", currency);
            model.addAttribute("users", users);
            return Mono.just("main");
        });
    }

    private void processAllFlashAttributes(Model model, Map<String, Object> flashAttributes, WebSession session) {
        if (flashAttributes == null) {
            return;
        }

        Map<String, String> attributeGroups = Map.of(
                "userAccountsErrors", "userAccountsSuccess",
                "passwordErrors", "passwordSuccess",
                "cashErrors", "cashSuccess",
                "transferErrors", "transferSuccess",
                "transferOtherErrors", "transferOtherSuccess"
        );

        attributeGroups.forEach((errorAttr, successAttr) ->
                processAttributeGroup(model, flashAttributes, errorAttr, successAttr)
        );

        session.getAttributes().remove("jakarta.servlet.flash.mapping.output");
    }

    private void processAttributeGroup(Model model, Map<String, Object> flashAttributes,
                                       String errorAttribute, String successAttribute) {
        Object errorsObj = flashAttributes.get(errorAttribute);
        if (errorsObj instanceof List) {
            List<?> errors = (List<?>) errorsObj;
            if (!errors.isEmpty()) {
                model.addAttribute(errorAttribute, errors);
                return;
            }
        }
    }
}
