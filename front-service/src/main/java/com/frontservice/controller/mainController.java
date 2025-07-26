package com.frontservice.controller;

import com.frontUi.api.DefaultApi;
import com.frontUi.domain.AccountForm;
import com.frontUi.domain.MainPageResponse;
import com.frontUi.domain.UserForm;
import com.frontservice.service.mainpageGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;
import java.util.stream.Stream;

@Controller
public class mainController {
    @Autowired
    DefaultApi defaultApi;
    @Autowired
    mainpageGetService mainpageGetService;
    @GetMapping()
    public Mono<String> redirect(Model model, Principal principal, ServerWebExchange exchange,Authentication authentication) {

        System.out.println(exchange.getRequest().getURI());
        System.out.println(exchange.getRequest().getQueryParams());
        System.out.println(exchange.getRequest().getHeaders());
        System.out.println(exchange.getRequest().getCookies());
        System.out.println(exchange.getRequest().getHeaders());
        System.out.println(exchange.getRequest().getAttributes().values());

        return defaultApi.apiGetMainPageGet().flatMap(dto -> {
            UserForm user = dto.getUser();
            List<AccountForm> accounts = dto.getAccounts();
            model.addAttribute("login", user.getLogin());
            System.out.println("login: " + user.getLogin());
            model.addAttribute("name", user.getName());
            model.addAttribute("birthdate", user.getBirthdate().toString());
            model.addAttribute("accounts", accounts);
            return Mono.just("main");
        });
    }
    @GetMapping("/bankapp")
    public Mono<String> main(Model model,ServerWebExchange exchange, Principal principal, Authentication authentication) {

        System.out.println(principal.getName());
        System.out.println(principal.toString());
        String principal3 = exchange.getPrincipal().toString();
        Authentication auth = (Authentication) principal;
        System.out.println(auth.getName());
        System.out.println(authentication.getName());
        System.out.println(principal3);


        return defaultApi.apiGetMainPageGet().flatMap(dto -> {
            UserForm user = dto.getUser();
            List<AccountForm> accounts = dto.getAccounts();
            model.addAttribute("login", user.getLogin());
            System.out.println("login: " + user.getLogin());
            model.addAttribute("name", user.getName());
            model.addAttribute("birthdate", user.getBirthdate().toString());
            model.addAttribute("accounts", accounts);
            return Mono.just("main");
        });
    }
}
