package com.exchangeservice.service;

import com.exchange_service.generated.get.domain.Transfer;
import com.exchange_service.generated.get.domain.TransferValue;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExchangeService {
    private final ExchangeGet exchangeGet;

    public ExchangeService(ExchangeGet exchangeGet) {
        this.exchangeGet = exchangeGet;
    }
    public Mono<TransferValue> getTransferValue(Mono<Transfer> transfer) {
        return transfer.flatMap(transfer1 -> {

            return exchangeGet.getExchange(transfer1.getFromCurrency(), transfer1.getToCurrency()).flatMap(aDouble -> {
                TransferValue transferValue = new TransferValue();
                double result = BigDecimal.valueOf(transfer1.getValue())
                        .divide(BigDecimal.valueOf(aDouble), 2, RoundingMode.HALF_UP)
                        .doubleValue();
                transferValue.setSummary(result);
                transferValue.setSuccess(true);
                return Mono.just(transferValue);
            });
        });
    }
}
