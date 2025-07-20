package com.accountservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testControler {
    @GetMapping("/api/test")
    public String test() {
        return "API Gateway is working!";
    }
}
