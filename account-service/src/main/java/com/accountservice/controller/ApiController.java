package com.accountservice.controller;


import com.account_service.generated.get.api.DefaultApi;
import com.account_service.generated.get.domain.*;
import com.accountservice.repository.UserRepository;
import com.accountservice.service.SignupService;
import com.accountservice.service.toApiDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    SignupService signupService;
    @Autowired
    private toApiDTO toApiDTO;

    @Autowired
    private UserRepository userRepository;


    @Override
    public Mono<ResponseEntity<SignupResponse>> apiSignupPost(Mono<RegistrationForm> registrationForm, ServerWebExchange exchange) {
        return signupService.signup(registrationForm).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MainPageResponse>> apiGetMainPageGet(ServerWebExchange exchange) {
        System.out.println("apiGetMainPageGet");
        String principal3 = exchange.getPrincipal().toString();
        System.out.println(principal3);
        System.out.println(exchange.getRequest().getURI());
        System.out.println(exchange.getRequest().getQueryParams());
        System.out.println(exchange.getRequest().getHeaders());
        System.out.println(exchange.getRequest().getCookies());
        System.out.println(exchange.getRequest().getHeaders());
        System.out.println(exchange.getRequest().getAttributes().values());

        return toApiDTO.getMAinPageDTO(exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserFormLogin>> apiGetUserPost(Mono<String> body, ServerWebExchange exchange) {
        return  toApiDTO.authUser(body).map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<EditUserResponse>> apiEditUserAccountsPost(Mono<UpdateUserForm> updateUserForm, ServerWebExchange exchange) {
        System.out.println("apiEditUserAccountsPost");
        return toApiDTO.editUser(updateUserForm, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> apiEditPasswordPost(Mono<PasswordChange> passwordChange, ServerWebExchange exchange) {
        return toApiDTO.editPassword(passwordChange, exchange.getRequest().getHeaders().getFirst("X-User-Name"));
    }

    @Override
    public Mono<ResponseEntity<AccountCashResponse>> transferToAccountService(Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        return toApiDTO.cash(cashTransfer, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TransferResponse>> transferFromToAccountService(Mono<Transfer> transfer, ServerWebExchange exchange) {
        return toApiDTO.transfer(transfer, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }
}
