package com.exchangegeneratorservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                )
                .oauth2Login(withDefaults())
                .oauth2ResourceServer(serverSpec -> serverSpec
                        .jwt(jwtSpec -> {
                            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                                List<String> realmRoles = realmAccess != null
                                        ? (List<String>) realmAccess.get("roles")
                                        : Collections.emptyList();

                                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                                List<String> clientRoles = Collections.emptyList();

                                if (resourceAccess != null) {
                                    Map<String, Object> accountResource = (Map<String, Object>) resourceAccess.get("account");
                                    if (accountResource != null) {
                                        clientRoles = (List<String>) accountResource.get("roles");
                                    }
                                }

                                List<String> allRoles = new ArrayList<>();
                                allRoles.addAll(realmRoles);
                                allRoles.addAll(clientRoles);
                                System.out.println(allRoles);
                                return Flux.fromIterable(allRoles)
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
                            });
                            jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
                );
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }


}
