package com.frontservice.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class mainpageGetService {
    public String getUserName(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;

            if (auth instanceof UsernamePasswordAuthenticationToken) {
                System.out.println("UsernamePasswordAuthenticationToken");
                return "DB_" + auth.getName();
            } else if (auth instanceof OAuth2AuthenticationToken) {
                System.out.println("UsernamePasswordAuthenticationToken");
                return "KC_" + auth.getName();
            }
        }
        return null;
    }
}
