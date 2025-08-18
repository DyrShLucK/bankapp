package com.blockerservice.service;

import com.blocker_service.domain.BlockerResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BlockerService {
    private final CircuitBreaker circuitBreaker;

    public BlockerService() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .build();

        this.circuitBreaker = CircuitBreaker.of("blockerService", config);
    }

    public Mono<BlockerResponse> checkSuspiciousOperation() {
        Mono<String> operationResult = Mono.fromSupplier(this::simulateOperation)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

        return operationResult
                .map(result -> {
                    BlockerResponse response = new BlockerResponse();
                    response.setSuccess(true);
                    response.setCause("Одобрено");
                    return response;
                })
                .onErrorResume(throwable -> {
                    BlockerResponse response = new BlockerResponse();
                    CircuitBreaker.State state = circuitBreaker.getState();
                    if (state == CircuitBreaker.State.OPEN || state == CircuitBreaker.State.HALF_OPEN) {
                        response.setSuccess(false);
                        response.setCause("Сервис заблокирован из-за подозрительной операции");
                    } else {
                        response.setSuccess(false);
                        response.setCause("Операция провалена");
                    }
                    return Mono.just(response);
                });
    }

    private String simulateOperation() {
        LocalTime now = LocalTime.now();

        if (now.getHour() >= 2 && now.getHour() < 4) {
            throw new RuntimeException("Suspicious time activity");
        }

        if (Math.random() < 0.3) {
            throw new RuntimeException("Random suspicious activity");
        }

        return "success";
    }
}
