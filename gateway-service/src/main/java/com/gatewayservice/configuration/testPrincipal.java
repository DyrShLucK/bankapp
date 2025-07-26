package com.gatewayservice.configuration;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;


public class testPrincipal {
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    @GetMapping()
    public Mono<String> test(ServerWebExchange exchange, Principal principal2) {
        return exchange.getPrincipal()
                .map(principal -> "Correct Manual Principal: " + principal.getName());
    }
    private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    public Mono<String> getUserAccessToken(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .ofType(OAuth2AuthenticationToken.class)
                .flatMap(auth -> authorizedClientRepository
                        .loadAuthorizedClient(
                                auth.getAuthorizedClientRegistrationId(),
                                auth,
                                exchange))
                .cast(OAuth2AuthorizedClient.class) // ← явное приведение
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue);
    }
}
