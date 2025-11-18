package com.exchangeservice.controller;

import com.exchange_service.generated.get.api.DefaultApi;
import com.exchange_service.generated.get.domain.Transfer;
import com.exchange_service.generated.get.domain.TransferValue;
import com.exchangeservice.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    ExchangeService exchangeService;
    @Override
    public Mono<ResponseEntity<TransferValue>> excangeservice(Mono<Transfer> transfer, ServerWebExchange exchange) {
        return exchangeService.getTransferValue(transfer).map(ResponseEntity::ok);
    }
}
