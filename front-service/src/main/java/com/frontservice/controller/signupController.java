package com.frontservice.controller;

import com.frontservice.DTO.RegistrationForm;
import com.frontservice.service.SignupApi;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class signupController {

    @Autowired
    SignupApi signupApi;
    @Autowired
    DiscoveryClient discoveryClient;

    @GetMapping("/signup")
    public Mono<String> showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        model.addAttribute("errors", java.util.Collections.emptyList());
        return Mono.just("signup");
    }

    @PostMapping("/signup")
    public Mono<String> register(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult result,
            Model model
    ) {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());

            model.addAttribute("registrationForm", form);
            model.addAttribute("errors", errorMessages);
            return Mono.just("signup");
        }
        return signupApi.signUp(form)
                .flatMap(signupResponse -> {
                    if (Boolean.TRUE.equals(signupResponse.getSuccess())) {
                        return Mono.just("redirect:/login");
                    } else {
                        List<String> errors = signupResponse.getCause() != null
                                ? List.of(signupResponse.getCause())
                                : List.of("Неизвестная ошибка");
                        model.addAttribute("registrationForm", form);
                        model.addAttribute("errors", errors);
                        return Mono.just("signup");
                    }
                });
    }
}
