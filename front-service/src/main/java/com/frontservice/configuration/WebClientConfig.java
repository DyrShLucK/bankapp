package com.frontservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter((request, next) -> {
                    // Копируем заголовки из текущего запроса
                    ServerHttpRequest currentRequest = ((ServerWebExchange) request.attributes().get("exchange")).getRequest();

                    // Копируем cookies
                    MultiValueMap<String, HttpCookie> cookies = currentRequest.getCookies();
                    String username = cookies.getFirst("username") != null ?
                            cookies.getFirst("username").getValue() : null;

                    // Создаем новый запрос с заголовками
                    ClientRequest.Builder newRequest = ClientRequest.from(request);

                    if (username != null) {
                        newRequest.header("X-User-Name", username);
                    }

                    // Копируем другие важные заголовки
                    currentRequest.getHeaders().forEach((name, values) -> {
                        if (name.startsWith("X-") || name.equals("Authorization")) {
                            newRequest.header(name, values.toArray(new String[0]));
                        }
                    });

                    return next.exchange(newRequest.build());
                })
                .build();
    }
}