package com.transferservice.configuration;

import com.transfer_service.generated.post.ApiClient;
import com.transfer_service.generated.post.api.DefaultApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class ApiConfiguration {
    @Autowired
    DiscoveryClient discoveryClient;
    @Value("${api.gateway.url:http://localhost:8080}")
    private String defaultGatewayUrl;
    @Bean
    public ApiClient apiClient() {
        return new ApiClient(WebClient.builder().build());
    }
    @Bean
    public DefaultApi defaultApi(WebClient.Builder webClientBuilder,
                                 ReactiveOAuth2AuthorizedClientManager clientManager,
                                 @Value("${oauth2.client.registration-id}") String clientId) {
        String gatewayUrl;

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances("gateway-service");
            if (instances != null && !instances.isEmpty()) {
                gatewayUrl = instances.getFirst().getUri().toString();
            } else {
                gatewayUrl = defaultGatewayUrl;
            }
        } catch (Exception e) {
            gatewayUrl = defaultGatewayUrl;
        }
        ApiClient apiClient = new ApiClient(
                webClientBuilder
                        .filter(oauthFilter(clientManager, clientId))
                        .build()
        ).setBasePath(gatewayUrl);
        return new DefaultApi(apiClient);

    }
    private ExchangeFilterFunction oauthFilter(
            ReactiveOAuth2AuthorizedClientManager clientManager,
            String clientId
    ) {
        return (request, next) ->
                // Получаем username из Reactor Context
                Mono.deferContextual(Mono::just)
                        .map(context -> context.getOrDefault(UserContextWebFilter.USER_NAME_KEY, "anonymous"))
                        .zipWith(getAccessToken(clientManager, clientId))
                        .flatMap(tuple -> {
                            String username = tuple.getT1();
                            String token = tuple.getT2();

                            ClientRequest newRequest = ClientRequest.from(request)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .header("X-User-Name", username)
                                    .build();

                            return next.exchange(newRequest);
                        });
    }

    private Mono<String> getAccessToken(
            ReactiveOAuth2AuthorizedClientManager clientManager,
            String clientId
    ) {
        return clientManager.authorize(
                        OAuth2AuthorizeRequest.withClientRegistrationId(clientId)
                                .principal("system")
                                .build()
                )
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue);
    }

    private ClientRequest withBearerToken(
            ClientRequest request,
            String token
    ) {
        return ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }
}
