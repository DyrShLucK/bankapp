package com.cashservice.get.controller;

import com.cash_service.generated.get.api.DefaultApi;
import com.cash_service.generated.get.domain.AccountCashResponse;
import com.cash_service.generated.get.domain.CashTransfer;
import com.cashservice.get.service.CashServiceGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class ApiControllerGet implements DefaultApi {
    @Autowired
    CashServiceGet cashService;
    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Override
    public Mono<ResponseEntity<AccountCashResponse>> processCashWithdrawal(@jakarta.annotation.Nullable String SESSION, Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        String username = extractUsernameFromSession(SESSION);
        if (username == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return cashService.cashFunc(cashTransfer, username, SESSION).map(ResponseEntity::ok);
    }

    private String extractUsernameFromSession(String sessionId) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String sessionKey = "spring:session:sessions:" + sessionId;

            String contextKey = "sessionAttr:SPRING_SECURITY_CONTEXT";
            byte[] sessionData = connection.hGet(
                    sessionKey.getBytes(StandardCharsets.UTF_8),
                    contextKey.getBytes(StandardCharsets.UTF_8)
            );

            if (sessionData == null) {
                return null;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(sessionData))) {
                Object obj = ois.readObject();

                SecurityContextImpl context = (SecurityContextImpl) obj;
                Authentication auth = context.getAuthentication();

                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    return username;
                } else {
                    if (principal instanceof String) {
                        return (String) principal;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
