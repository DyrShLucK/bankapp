package com.notificationsservice;

import com.notificationsservice.kafka.ResponseNotificationConsumer;
import com.notificationsservice.model.Notification;
import com.notificationsservice.service.ApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResponseNotificationConsumerTest {

    @Mock
    private ApiService apiService;

    private ResponseNotificationConsumer responseNotificationConsumer;

    @BeforeEach
    void setUp() {
        responseNotificationConsumer = new ResponseNotificationConsumer();
        setApiService(responseNotificationConsumer, apiService);
    }

    private void setApiService(ResponseNotificationConsumer consumer, ApiService service) {
        try {
            java.lang.reflect.Field field = ResponseNotificationConsumer.class.getDeclaredField("apiService");
            field.setAccessible(true);
            field.set(consumer, service);
        } catch (Exception e) {
            throw new RuntimeException("Could not set apiService field", e);
        }
    }

    @Test
    void testConsumeResponseNotification_Success() {
        Notification notification = new Notification("testUser", "Test message");
        String username = "testUser";

        responseNotificationConsumer.consumeResponseNotification(notification, username);

        verify(apiService, times(1))
                .storeNotificationForUser(eq(username), eq(notification));
    }

    @Test
    void testConsumeResponseNotification_WithNullKeyUsesPayloadUsername() {
        Notification notification = new Notification("payloadUser", "Test message");

        responseNotificationConsumer.consumeResponseNotification(notification, null);

        verify(apiService, times(1))
                .storeNotificationForUser(eq("payloadUser"), eq(notification));
    }

    @Test
    void testConsumeResponseNotification_WithNullKeyAndNullPayloadUsername() {
        Notification notification = new Notification();
        notification.setMessage("Test message");

        responseNotificationConsumer.consumeResponseNotification(notification, null);

        verify(apiService, times(0))
                .storeNotificationForUser(any(), any());
    }
}