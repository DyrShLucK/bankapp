package com.frontservice.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontservice.model.KafkaNotificationRequest;
import com.frontservice.model.KafkaNotificationResponse;
import com.frontservice.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); // username -> session
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Предположим, username передаётся как часть URI или через handshake
        // Пример: ws://localhost:8080/websocket/notifications?username=john_doe
        String username = extractUsername(session);
        if (username != null && session.isOpen()) {
            sessions.put(username, session);
            logger.info("WebSocket session established for user: {}", username);

            // Отправляем запрос в Kafka сразу после подключения
            // (Или по таймеру, как в вашем старом коде)
            sendNotificationRequestToKafka(username);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().removeIf(s -> s.getId().equals(session.getId()));
        logger.info("WebSocket session closed for session ID: {}", session.getId());
    }

    // Kafka Listener для получения уведомлений от notifications-service
    @KafkaListener(topics = "notifications.responses", groupId = "frontservice-websocket-group")
    public void handleNotificationResponse(KafkaNotificationResponse response) {
        String username = response.getUsername();
        WebSocketSession session = sessions.get(username);

        if (session != null && session.isOpen()) {
            try {
                // Преобразуем KafkaNotificationResponse в JSON строку
                String jsonResponse = objectMapper.writeValueAsString(response.getNotifications());
                session.sendMessage(new TextMessage(jsonResponse));
                logger.debug("Sent notifications to WebSocket for user: {}", username);
            } catch (IOException e) {
                logger.error("Error sending message to WebSocket for user: {}", username, e);
                // Возможно, стоит удалить сессию из мэпа, если она больше не валидна
                sessions.remove(username);
            }
        } else {
            logger.warn("No active WebSocket session found for user: {} to send notification", username);
            // Уведомление потеряно, если сессия не активна
        }
    }

    private String extractUsername(WebSocketSession session) {
        // Пример извлечения username из URI параметров
        // URI: ws://localhost:8080/websocket/notifications?username=john_doe
        Map<String, Object> attributes = session.getAttributes();
        String username = (String) attributes.get("username");
        if (username == null) {
            // Попробовать получить из URI
            var uri = session.getUri();
            if (uri != null) {
                var query = uri.getQuery();
                if (query != null) {
                    var params = java.util.Arrays.stream(query.split("&"))
                            .map(s -> s.split("="))
                            .collect(java.util.stream.Collectors.toMap(a -> a[0], a -> a[1]));
                    username = params.get("username");
                }
            }
        }
        return username;
    }

    private void sendNotificationRequestToKafka(String username) {
        KafkaNotificationRequest request = new KafkaNotificationRequest(username);
        try {
            kafkaTemplate.send("notifications.requests", username, request);
            logger.debug("Sent notification request to Kafka for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to send notification request to Kafka for user: {}", username, e);
        }
    }
}