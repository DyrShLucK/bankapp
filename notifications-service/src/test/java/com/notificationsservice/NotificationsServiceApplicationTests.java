package com.notificationsservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest(
		properties = {
				"spring.kafka.consumer.bootstrap-servers=localhost:9092",
				"kafka.topics.notificationsRequests=notifications.requests"

		}
)
@Disabled("Disabled until Kafka setup is fixed for tests")
class NotificationsServiceApplicationTests {
	@MockBean
	private ReactiveJwtDecoder jwtDecoder;
	@MockBean
	private ReactiveOAuth2AuthorizedClientService authorizedClientService;
	@MockBean
	private ReactiveClientRegistrationRepository clientRegistrationRepository;
	@Test
	void contextLoads() {
	}

}
