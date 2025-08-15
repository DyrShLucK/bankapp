package com.blockerservice.service;

import com.blocker_service.domain.BlockerResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class BlockerService {
    private final AtomicLong requestCounter = new AtomicLong(0);

    public Mono<BlockerResponse> getBlocker() {
        return Mono.fromCallable(() -> {
            long currentRequest = requestCounter.incrementAndGet();

            BlockerResponse response = new BlockerResponse();

            if (currentRequest % 3 == 0) {
                response.setSuccess(false);
                response.setCause("Подозрительная операция - автоматическая блокировка каждого третьего запроса");
            } else {
                response.setSuccess(true);
                response.setCause("Операция разрешена");
            }

            return response;
        });
    }
}
