package com.gatewayservice.configuration;

import com.gateway.api.DefaultApi;
import com.gateway.domain.UserFormLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private DiscoveryClient discoveryClient;


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/signup").permitAll()
                        .anyExchange().permitAll()
                )
                .formLogin(form -> form
                        .authenticationSuccessHandler((exchange, authentication) -> {
                            exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders().setLocation(URI.create("/"));
                            return exchange.getExchange().getResponse().setComplete();
                        })
                )
                .oauth2Client(withDefaults())
                .oauth2ResourceServer(serverSpec -> serverSpec
                        .jwt(jwtSpec -> {
                            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                                List<String> roles = jwt.getClaim("roles");
                                return Flux.fromIterable(roles != null ? roles : List.of())
                                        .map(SimpleGrantedAuthority::new);
                            });

                            jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
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
                                            .path(cookie.getPath() != null ? cookie.getPath() : "/")
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
                )
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler((exchange, denied) -> {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.FOUND);
                            response.getHeaders().setLocation(URI.create("/"));
                            return response.setComplete();
                        })
                );
        return http.build();
    }

//    public ApplicationRunner initUsers(PasswordEncoder encoder) {
//        return args -> {
//            userRepository.findByUsername("user")
//                    .switchIfEmpty(userRepository.save(
//                            new com.accountservice.model.User( "user123", encoder.encode("user123"),"Ivan", LocalDate.now(), "USER")
//                    ))
//                    .subscribe();
//
//            userRepository.findByUsername("manager")
//                    .switchIfEmpty(userRepository.save(
//                            new com.accountservice.model.User("manager", encoder.encode("manager"),"Ivan", LocalDate.now(), "MANAGER")
//                    ))
//                    .subscribe();
//        };
//    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(DefaultApi defaultApi) {
        return username -> defaultApi.apiGetUserPost(username).map(user -> toUserDetails(user, username));

    }
    private UserDetails toUserDetails(UserFormLogin user, String originalUsername) {
        return User.builder()
                .username(originalUsername)
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    @Bean
    public FormHttpMessageReader formHttpMessageReader() {
        return new FormHttpMessageReader();
    }
    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository(ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService);
    }
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        var oidcUserService = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = oidcUserService.loadUser(userRequest);

            String prefixedUsername = "kc_" + oidcUser.getPreferredUsername();

            List<String> roles = oidcUser.getClaimAsStringList("spring_sec_roles");
            Collection<GrantedAuthority> authorities = Stream.concat(
                    oidcUser.getAuthorities().stream(),
                    roles.stream().map(SimpleGrantedAuthority::new)
            ).collect(Collectors.toList());

            return new DefaultOidcUser(
                    authorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo(),
                    prefixedUsername
            );
        };
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);

        handler.setPostLogoutRedirectUri(discoveryClient.getInstances("gateway-service").getFirst().getUri().toString());

        return handler;
    }
    @Bean
    ReactiveOAuth2AuthorizedClientManager auth2AuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .refreshToken()
                .build()
        );

        return manager;
    }



}
