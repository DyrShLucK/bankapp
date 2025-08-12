package com.transferservice.controller;

import com.transfer_service.generated.get.api.DefaultApi;
import com.transfer_service.generated.get.domain.Transfer;
import com.transfer_service.generated.get.domain.TransferResponse;
import com.transferservice.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@RestController
public class ApiController implements DefaultApi {

    @Autowired
    ApiService apiService;

    @Override
    public Mono<ResponseEntity<TransferResponse>> apiTransferPost(Mono<Transfer> transfer, ServerWebExchange exchange) {
        System.out.println("apiTransferPost");
        return apiService.getTransferResponse(transfer, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }
}
