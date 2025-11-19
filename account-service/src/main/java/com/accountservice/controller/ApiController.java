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
    public Mono<ResponseEntity<MainPageResponse>> apiGetMainPageGet(@jakarta.annotation.Nullable String SESSION, ServerWebExchange exchange) {
        log.info("Received SESSION: {}", SESSION);

        if (SESSION == null || SESSION.trim().isEmpty()) {
            log.error("SESSION cookie is missing or empty");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String username = extractUsernameFromSession(SESSION);

        if (username == null) {
            log.error("Username not found in session: {}", SESSION);
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        log.info("Successfully extracted username: {} from session: {}", username, SESSION);

        return toApiDTO.getMAinPageDTO(username)
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
                                    .secure(true)  // Обязательно true в продакшене с HTTPS
                                    .path("/")
                                    .maxAge(3600)
                                    .domain("bankapp.internal")  // Укажите ваш домен
                                    .sameSite("Lax")  // Важно для кросс-доменных запросов
                                    .build();

                    exchange.getResponse().addCookie(userCookie);

                    headers.forEach((headerName, headerValues) ->
                            log.info("Response Header {}: {}", headerName, String.join(", ", headerValues)));

                    return Mono.just(ResponseEntity.ok()
                            .headers(headers)
                            .body(userFormLogin));
                });
    }





    @Override
    public Mono<ResponseEntity<Void>> apiEditPasswordPost(String SESSION, Mono<PasswordChange> passwordChange, ServerWebExchange exchange) {
        String username = extractUsernameFromSession(SESSION);
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.editPassword(passwordChange, username);
    }

    @Override
    public Mono<ResponseEntity<EditUserResponse>> apiEditUserAccountsPost(String SESSION, Mono<UpdateUserForm> updateUserForm, ServerWebExchange exchange) {
        String username = extractUsernameFromSession(SESSION);
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.editUser(updateUserForm, username).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountCashResponse>> transferToAccountService(String SESSION, Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        String username = extractUsernameFromSession(SESSION);
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.cash(cashTransfer, username).map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<TransferResponse>> transferFromToAccountService(String SESSION, Mono<Transfer> transfer, ServerWebExchange exchange) {
        String username = extractUsernameFromSession(SESSION);
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return toApiDTO.transfer(transfer, username).map(ResponseEntity::ok);
    }




    private String extractUsernameFromSession(String sessionId) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String sessionKey = "spring:session:sessions:" + sessionId;
            log.info("Trying to access Redis key: {}", sessionKey);

            String contextKey = "sessionAttr:SPRING_SECURITY_CONTEXT";
            byte[] sessionData = connection.hGet(
                    sessionKey.getBytes(StandardCharsets.UTF_8),
                    contextKey.getBytes(StandardCharsets.UTF_8)
            );

            if (sessionData == null) {
                log.error("Session data not found for ID: {}", sessionId);
                return null;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(sessionData))) {
                Object obj = ois.readObject();
                log.info("Deserialized object: {}", obj.getClass().getName());

                SecurityContextImpl context = (SecurityContextImpl) obj;
                Authentication auth = context.getAuthentication();
                log.info("Authentication: {}", auth);

                Object principal = auth.getPrincipal();
                log.info("Principal class: {}", principal.getClass().getName());
                log.info("Principal: {}", principal);

                // Проверяем тип principal
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    log.info("Extracted username: {}", username);
                    return username;
                } else {
                    log.warn("Principal is not of type User, it's: {}", principal.getClass().getName());
                    // Если principal - строка, возвращаем как есть
                    if (principal instanceof String) {
                        return (String) principal;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting username from session: {}", e.getMessage(), e);
            return null;
        }
        return null;
    }
}
