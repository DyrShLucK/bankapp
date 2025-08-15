package com.exchangeservice.service;

import com.exchange_service.generated.post.api.DefaultApi;
import com.exchange_service.generated.post.domain.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ExchangeGet {
    private final DefaultApi defaultApi;

    public ExchangeGet(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }
    public Mono<Double> getExchange(String from, String to) {
        return defaultApi.apiExchangeGet()
                .collectList()
                .map(exchangeList -> {
                    Double fromRate = 1.0;
                    Double toRate = 1.0;

                    for (Value value : exchangeList) {
                        if (value.getTitle().equals(from)) {
                            fromRate = value.getCost();
                        }
                        if (value.getTitle().equals(to)) {
                            toRate = value.getCost();
                        }
                    }
                    return toRate / fromRate;
                });
    }
}
