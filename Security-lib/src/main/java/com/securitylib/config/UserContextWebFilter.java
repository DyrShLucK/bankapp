package com.securitylib.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class UserContextWebFilter implements WebFilter {

    public static final String USER_NAME_KEY = "username";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String username = exchange.getRequest().getHeaders().getFirst("X-User-Name");
        if (username == null || username.isBlank()) {
            username = "anonymous";
        }

        String finalUsername = username;
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(USER_NAME_KEY, finalUsername));
    }
}
