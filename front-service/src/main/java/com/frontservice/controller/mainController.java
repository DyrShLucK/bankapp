package com.frontservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontUi.api.DefaultApi;
import com.frontUi.domain.*;
import org.eclipse.angus.mail.util.DecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    private static final Logger log = LoggerFactory.getLogger(mainController.class);

    @GetMapping({"/", "/bankapp"})
    public Mono<String> main(Model model, ServerWebExchange exchange, WebSession session) {

        Object authAttribute = session.getAttributes().get("SPRING_SECURITY_CONTEXT");
        org.springframework.security.core.userdetails.User userDetails = null;
        if (authAttribute != null) {

            if (authAttribute instanceof org.springframework.security.core.context.SecurityContextImpl) {
                org.springframework.security.core.context.SecurityContextImpl securityContext =
                        (org.springframework.security.core.context.SecurityContextImpl) authAttribute;
                org.springframework.security.core.Authentication authentication = securityContext.getAuthentication();

                if (authentication != null && authentication.getPrincipal() != null) {
                    if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                        userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                    }
                }
            }
        } else {
            log.info("SPRING_SECURITY_CONTEXT not found in session");
        }
        return defaultApi.apiGetMainPageGet(userDetails.getUsername())
                .flatMap(dto -> {
                    Map<String, Object> flashAttributes = (Map<String, Object>) session.getAttributes().get("jakarta.servlet.flash.mapping.output");
                    processAllFlashAttributes(model, flashAttributes, session);

                    UserForm user = dto.getUser();
                    List<AccountForm> accounts = dto.getAccounts();
                    List<Currency> currency = dto.getCurrencys();
                    List<Users> users = dto.getUsers();
                    model.addAttribute("login", dto.getUser().getLogin());
                    model.addAttribute("name", dto.getUser().getName());
                    model.addAttribute("birthdate", user.getBirthdate().toString());
                    model.addAttribute("accounts", accounts);
                    model.addAttribute("currency", currency);
                    model.addAttribute("users", users);
                    return Mono.just("main");
                })
                .onErrorResume(error -> {
                    log.error("Ошибка при получении главной страницы", error);
                    return Mono.just("redirect:/login");
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
