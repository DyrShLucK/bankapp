package com.gatewayservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import java.net.URI;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("signup").permitAll()
                        .anyExchange().permitAll()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(withDefaults())
                )
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((exchange, authentication) -> {
                    ServerWebExchange serverWebExchange = exchange.getExchange();
                    oidcLogoutSuccessHandler();
                    serverWebExchange.getSession()
                            .flatMap(WebSession::invalidate)
                            .subscribe();

                    serverWebExchange.getResponse().getCookies().forEach((name, cookies) -> {
                        cookies.forEach(cookie -> {
                            ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                                    .maxAge(0)
                                    .path(cookie.getPath() != null ? cookie.getPath() : "/signup")
                                    .domain(cookie.getDomain())
                                    .secure(cookie.isSecure())
                                    .httpOnly(cookie.isHttpOnly())
                                    .sameSite(cookie.getSameSite())
                                    .build();

                            serverWebExchange.getResponse().addCookie(deleteCookie);
                        });
                    });

                    serverWebExchange.getPrincipal()
                            .filter(principal -> principal instanceof Authentication)
                            .map(principal -> (Authentication) principal)
                            .doOnNext(auth -> auth.setAuthenticated(false))
                            .subscribe();

                    ServerHttpResponse response = serverWebExchange.getResponse();
                    response.setStatusCode(HttpStatus.FOUND);
                    response.getHeaders().setLocation(URI.create("/login?logout"));

                    return response.setComplete();
                }).logoutSuccessHandler(oidcLogoutSuccessHandler())
        );
        return http.build();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);

        handler.setPostLogoutRedirectUri("http://localhost:8080");

        return handler;
    }
}
