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
    public Mono<ResponseEntity<AccountCashResponse>> processCashWithdrawal(@jakarta.annotation.Nullable String userName, Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {

        if (userName == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return cashService.cashFunc(cashTransfer, userName, userName).map(ResponseEntity::ok);
    }


}
