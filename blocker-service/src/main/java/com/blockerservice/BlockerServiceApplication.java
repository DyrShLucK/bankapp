package com.blockerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({com.securitylib.config.SecurityConfiguration.class, com.securitylib.config.UserContextWebFilter.class})
public class BlockerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockerServiceApplication.class, args);
    }

}
