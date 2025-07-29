package com.cashservice.get.controller;

import com.cash_service.generated.get.api.DefaultApi;
import com.cash_service.generated.get.domain.AccountCashResponse;
import com.cash_service.generated.get.domain.CashTransfer;
import com.cashservice.get.service.CashServiceGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiControllerGet implements DefaultApi {
    @Autowired
    CashServiceGet cashService;


    @Override
    public Mono<ResponseEntity<AccountCashResponse>> processCashWithdrawal(Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        return cashService.cashFunc(cashTransfer).map(ResponseEntity::ok);
    }
}
