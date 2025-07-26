package com.gatewayservice.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Component
public class AddUserHeaderFilter implements GlobalFilter, Ordered {
    private static final String USER_HEADER_NAME = "X-User-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Проверяем, есть ли уже заголовок X-User-Name
        if (request.getHeaders().containsKey(USER_HEADER_NAME)) {
            // Если заголовок уже есть — ничего не делаем
            System.out.println("Header " + USER_HEADER_NAME + " already exists. Skipping.");
            return chain.filter(exchange);
        }

        // Если заголовка нет — пытаемся получить Principal
        return exchange.getPrincipal()
                .map(principal -> {
                    String username = principal.getName();
                    System.out.println("Adding header " + USER_HEADER_NAME + ": " + username);

                    // Добавляем заголовок с именем пользователя
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(USER_HEADER_NAME, username)
                            .build();

                    // Создаём новый exchange с модифицированным запросом
                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(modifiedRequest)
                            .build();

                    return modifiedExchange;
                })
                .switchIfEmpty(Mono.just(exchange)) // Если нет Principal — продолжаем без изменений
                .flatMap(chain::filter); // Передаём дальше по цепочке
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Высший приоритет
    }
}
