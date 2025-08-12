package com.accountservice.service;

import com.account_service.generated.get.domain.RegistrationForm;
import com.account_service.generated.get.domain.SignupResponse;
import com.accountservice.model.User;
import com.accountservice.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SignupService {

    private final UserRepository userRepository;

    public SignupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<SignupResponse> signup(Mono<RegistrationForm> registrationForm) {
       return registrationForm.flatMap(form -> {
            String login = form.getLogin();
            return userRepository.findByUsername(login).map(user -> {
                SignupResponse signupResponse = new SignupResponse();
                signupResponse.setSuccess(Boolean.FALSE);
                signupResponse.setCause("Логин уже занят");
                return signupResponse;
            }).switchIfEmpty(Mono.defer(() ->{
                User user = new User();
                user.setUsername(form.getLogin());
                user.setPassword(form.getPassword());
                user.setName(form.getName());
                user.setBirthday(form.getBirthdate());
                user.setRole("USER");
                return userRepository.save(user).map(saveduser -> {
                    SignupResponse signupResponse = new SignupResponse();
                    signupResponse.setSuccess(Boolean.TRUE);
                    return signupResponse;
                });
            }));
        });
    }
}
