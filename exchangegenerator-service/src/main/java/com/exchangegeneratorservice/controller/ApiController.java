package com.exchangegeneratorservice.controller;

import com.exchange_service.api.DefaultApi;
import com.exchange_service.domain.Value;
import com.exchangegeneratorservice.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    ApiService apiService;
    @Override
    public Mono<ResponseEntity<Flux<Value>>> apiExchangeGet(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(apiService.getExchangeRates()));
    }
}
