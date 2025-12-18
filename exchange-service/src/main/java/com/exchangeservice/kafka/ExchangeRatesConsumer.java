package com.exchangeservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.exchange_service.generated.post.domain.Value;
import com.exchangeservice.service.ExchangeGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExchangeRatesConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRatesConsumer.class);

    @Autowired
    private ExchangeGet exchangeGet;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "exchange.rates", groupId = "exchange-service-group")
    public void consumeExchangeRates(List rawRates, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            logger.info("Received exchange rates from Kafka topic 'exchange.rates', key: {}, rates count: {}",
                    key, rawRates.size());

            // Преобразование raw List в List<Value>
            List<Value> rates = objectMapper.convertValue(rawRates, new TypeReference<List<Value>>() {});

            logger.info("Converted {} raw objects to Value objects", rates.size());

            // Обновляем кэш курсов в ExchangeGet
            exchangeGet.updateCachedRates(rates);

            logger.info("Exchange rates successfully updated from Kafka message");

        } catch (Exception e) {
            logger.error("Failed to process exchange rates received from Kafka topic 'exchange.rates'", e);
        }
    }
}