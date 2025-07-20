package com.accountservice.controller;


import com.account_service.api.DefaultApi;
import com.account_service.domain.RegistrationForm;
import com.account_service.domain.SignupResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {

    @Override
    public Mono<ResponseEntity<SignupResponse>> apiSignupPost(Mono<RegistrationForm> registrationForm, ServerWebExchange exchange) {
        return registrationForm.doOnNext(form -> {
                    System.out.println("Login: " + form.getLogin());
                })
                .map(form -> {
                    SignupResponse signupResponse = new SignupResponse();
                    signupResponse.setSuccess(false);
                    signupResponse.setCause("логин занят");
                    return ResponseEntity.ok(signupResponse);
                });
    }
}
