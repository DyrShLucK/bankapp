package com.blockerservice.controller;

import com.blocker_service.api.DefaultApi;
import com.blocker_service.domain.BlockerResponse;
import com.blockerservice.service.BlockerService;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    BlockerService blockerService;
    @Override
    public Mono<ResponseEntity<BlockerResponse>> apiBlockerGet(ServerWebExchange exchange) {
        return blockerService.checkSuspiciousOperation().map(ResponseEntity::ok);
    }
}
