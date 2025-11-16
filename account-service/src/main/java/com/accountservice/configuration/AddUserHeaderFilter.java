package com.accountservice.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AddUserHeaderFilter implements GlobalFilter, Ordered {
    private static final String USER_HEADER_NAME = "X-User-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("=======================================================");
        System.out.println(exchange.getRequest().getHeaders().toString());
        System.out.println("=======================================================");
        ServerHttpRequest request = exchange.getRequest();

        if (request.getHeaders().containsKey(USER_HEADER_NAME)) {
            System.out.println("if");
            return chain.filter(exchange);
        }
        System.out.println("else");
        return exchange.getPrincipal()
                .map(principal -> {
                    String username = principal.getName();
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(USER_HEADER_NAME, username)
                            .build();

                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(modifiedRequest)
                            .build();

                    return modifiedExchange;
                })
                .switchIfEmpty(Mono.just(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
