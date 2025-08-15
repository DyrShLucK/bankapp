package com.exchangegeneratorservice.service;

import com.exchange_service.domain.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ApiService {
    private List<Value> cachedRates = new ArrayList<>();
    private long lastUpdate = 0;
    private final long CACHE_DURATION = 60000;
    private final Random random = new Random();

    public Flux<Value> getExchangeRates() {
        long currentTime = System.currentTimeMillis();

        if (cachedRates.isEmpty() || (currentTime - lastUpdate) > CACHE_DURATION) {
            updateCachedRates();
            lastUpdate = currentTime;
        }

        return Flux.fromIterable(cachedRates);
    }

    private void updateCachedRates() {
        cachedRates = new ArrayList<>();

        Value value = new Value();
        value.setCost(1.0);
        value.setName("Российский рубль");
        value.setTitle("RUB");
        cachedRates.add(value);

        Value value1 = new Value();
        value1.setCost(Math.round((80.0 + random.nextDouble() * 4.5) * 100.0) / 100.0);
        value1.setName("Доллар США");
        value1.setTitle("USD");
        cachedRates.add(value1);

        Value value2 = new Value();
        value2.setCost(Math.round((90.0 + random.nextDouble() * 5) * 100.0) / 100.0);
        value2.setName("Евро");
        value2.setTitle("EUR");
        cachedRates.add(value2);
    }
}
