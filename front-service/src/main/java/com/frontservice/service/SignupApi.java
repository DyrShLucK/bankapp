package com.frontservice.service;

import com.frontUi.api.DefaultApi;
import com.frontUi.domain.SignupResponse;
import com.frontservice.DTO.RegistrationForm;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SignupApi {

    private final DefaultApi defaultApi;
    public SignupApi(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }

    public Mono<SignupResponse> signUp(RegistrationForm registrationForm ) {
        com.frontUi.domain.RegistrationForm form = new com.frontUi.domain.RegistrationForm();
        form.setName(registrationForm.getName());
        form.setPassword(registrationForm.getPassword());
        form.setConfirmPassword(registrationForm.getPassword());
        form.setBirthdate(registrationForm.getBirthdate());
        form.setLogin(registrationForm.getLogin());
        return  defaultApi.apiSignupPost(form);
    }

}
