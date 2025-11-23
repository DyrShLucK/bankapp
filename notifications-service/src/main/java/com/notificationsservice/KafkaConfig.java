package com.notificationsservice;

import com.notificationsservice.model.*;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // --- Topic Definitions ---
    @Bean
    public NewTopic notificationsRequestsTopic() {
        return TopicBuilder.name("notifications.requests")
                .partitions(1) // Настройте количество партиций по необходимости
                .replicas(1)   // Настройте количество реплик по необходимости, особенно в проде
                .build();
    }

    @Bean
    public NewTopic notificationsResponsesTopic() {
        return TopicBuilder.name("notifications.responses")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // --- Producer Configuration ---
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Настройки для At-least-once delivery (для продюсера)
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // или "1" для менее строгой гарантии
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // Максимальное количество попыток
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Важно при retries > 1, чтобы сохранить порядок
        // props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Альтернатива max.in.flight.requests.per.connection
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // --- Consumer Configuration ---
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notifications-service-group"); // Уникальный ID группы для этого сервиса
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.notificationsservice.domain"); // Пакет с вашими DTO

        // Настройки для At-least-once delivery (для консьюмера)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // ВАЖНО
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // Начинаем с последнего сообщения при первом запуске, если оффсеты не найдены

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Устанавливаем AckMode.MANUAL_IMMEDIATE, чтобы подтверждать оффсеты вручную сразу после обработки
        // Это обеспечивает At-least-once, так как если обработка упадёт, оффсет не будет подтверждён
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}