package com.notificationsservice.config;

import com.notification_service.domain.Notification;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    /**
     * Фабрика ConsumerFactory для обработки уведомлений из responses.
     * Использует внутренний класс Notification и отключает type mapping.
     */
    @Bean
    public ConsumerFactory<String, Notification> responsesConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka.kafka-dev.svc.cluster.local:9092");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "notification-service-responses-group");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        JsonDeserializer<Notification> jsonDeserializer = new JsonDeserializer<>(Notification.class, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    /**
     * Фабрика ContainerFactory для обработки уведомлений из responses.
     * Использует responsesConsumerFactory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Notification> responsesKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Notification> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(responsesConsumerFactory());
        return factory;
    }
}