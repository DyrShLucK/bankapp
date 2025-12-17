package com.accountservice.controller;


import com.account_service.generated.get.api.DefaultApi;
import com.account_service.generated.get.domain.*;
import com.accountservice.model.User;
import com.accountservice.repository.UserRepository;
import com.accountservice.service.SignupService;
import com.accountservice.service.ApiServiceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    SignupService signupService;
    @Autowired
    private ApiServiceDTO toApiDTO;
    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @Override
    public Mono<ResponseEntity<SignupResponse>> apiSignupPost(Mono<RegistrationForm> registrationForm, ServerWebExchange exchange) {
        return signupService.signup(registrationForm).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MainPageResponse>> apiGetMainPageGet(@jakarta.annotation.Nullable String userName, ServerWebExchange exchange) {



        if (userName == null) {
            log.error("Username not found in session: {}", userName);
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }


        return toApiDTO.getMAinPageDTO(userName)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }



    @Override
    public Mono<ResponseEntity<UserFormLogin>> apiGetUserPost(Mono<String> body, ServerWebExchange exchange) {
        return toApiDTO.authUser(body)
                .flatMap(userFormLogin -> {
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

                    // Добавляем заголовки для совместимости (если нужно)
                    headers.add("X-User-Name", userFormLogin.getLogin());
                    headers.add("X-User-Role", userFormLogin.getRole());
                    headers.add("X-User-Authenticated", "true");

                    // Создаем cookie с логином пользователя
                    org.springframework.http.ResponseCookie userCookie =
                            org.springframework.http.ResponseCookie.from("username", userFormLogin.getLogin())
                                    .httpOnly(true)
                                    .secure(true)
                                    .path("/")
                                    .maxAge(3600)
                                    .domain("bankapp.internal")
                                    .sameSite("Lax")
                                    .build();

                    exchange.getResponse().addCookie(userCookie);



                    return Mono.just(ResponseEntity.ok()
                            .headers(headers)
                            .body(userFormLogin));
                });
    }





    @Override
    public Mono<ResponseEntity<Void>> apiEditPasswordPost(String userName, Mono<PasswordChange> passwordChange, ServerWebExchange exchange) {
        if (userName == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.editPassword(passwordChange, userName);
    }


    @Override
    public Mono<ResponseEntity<EditUserResponse>> apiEditUserAccountsPost(String userName, Mono<UpdateUserForm> updateUserForm, ServerWebExchange exchange) {
        log.info("Received request to edit user accounts and user info. Username: {}", userName);
        System.out.println("Received request to edit user accounts and user info. Username: " + userName);

        return updateUserForm.doOnNext(form -> {
            log.info("Received UpdateUserForm: {}", form);
        }).then(Mono.defer(() -> {
            if (userName == null) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            return toApiDTO.editUser(updateUserForm, userName).map(ResponseEntity::ok);
        }));
    }

    @Override
    public Mono<ResponseEntity<AccountCashResponse>> transferToAccountService(String userName, Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        if (userName == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.cash(cashTransfer, userName).map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<TransferResponse>> transferFromToAccountService(String userName, Mono<Transfer> transfer, ServerWebExchange exchange) {
        if (userName == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.transfer(transfer, userName).map(ResponseEntity::ok);
    }




}
