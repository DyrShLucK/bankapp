package com.frontservice.configuration;

import com.frontUi.ApiClient;
import com.frontUi.api.DefaultApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ApiConfiguration {
    @Autowired
    DiscoveryClient discoveryClient;
    @Bean
    public ApiClient apiClient() {return new ApiClient(WebClient.builder().build());}
    @Bean
    public DefaultApi defaultApi() {
        return new DefaultApi(apiClient().setBasePath(discoveryClient.getInstances("gateway-service").getFirst().getUri().toString()));

    }
}
