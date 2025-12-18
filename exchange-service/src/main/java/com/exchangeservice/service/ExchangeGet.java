package com.exchangeservice.service;

import com.exchange_service.generated.post.api.DefaultApi;
import com.exchange_service.generated.post.domain.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ExchangeGet {
    private final DefaultApi defaultApi;

    private volatile List<Value> cachedRates = new CopyOnWriteArrayList<>();
    private volatile long lastUpdate = 0;
    private final long CACHE_DURATION = 60000;

    public ExchangeGet(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }

    public Mono<Double> getExchange(String from, String to) {
        long currentTime = System.currentTimeMillis();

        if (cachedRates.isEmpty() || (currentTime - lastUpdate) > CACHE_DURATION) {
            return defaultApi.apiExchangeGet()
                    .collectList()
                    .doOnNext(this::updateCachedRates)
                    .map(exchangeList -> calculateExchangeRate(exchangeList, from, to));
        } else {
            return Mono.just(calculateExchangeRate(cachedRates, from, to));
        }
    }

    private Double calculateExchangeRate(List<Value> rates, String from, String to) {
        Double fromRate = 1.0;
        Double toRate = 1.0;

        for (Value value : rates) {
            if (value.getTitle().equals(from)) {
                fromRate = value.getCost();
            }
            if (value.getTitle().equals(to)) {
                toRate = value.getCost();
            }
        }
        return toRate / fromRate;
    }

    public void updateCachedRates(List<Value> newRates) {
        if (newRates != null && !newRates.isEmpty()) {
            this.cachedRates = new CopyOnWriteArrayList<>(newRates);
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}