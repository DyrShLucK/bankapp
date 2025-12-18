package com.exchangegeneratorservice.service;

import com.exchange_service.domain.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendExchangeRates(List<Value> rates) {
        try {

            kafkaTemplate.send("exchange.rates", "rates", rates);

        } catch (Exception e) {
            logger.error("Failed to send exchange rates to Kafka", e);
        }
    }
}