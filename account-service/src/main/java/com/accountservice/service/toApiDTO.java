package com.accountservice.service;

import com.account_service.domain.*;
import com.accountservice.model.Account;
import com.accountservice.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

@Service
public class toApiDTO {

    private final AccountService accountService;
    private final UserRepository userRepository;

    public toApiDTO(AccountService accountService, UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    public Mono<MainPageResponse> getMAinPageDTO(String username) {
        return Mono.just(username)
                .flatMap(usernameStr ->
                        Mono.zip(
                                createUserForm(usernameStr),
                                accountService.findOrCreateAccounts(usernameStr)
                                        .map(this::convertToAccountForm)
                                        .collectList()
                        ).map(tuple -> createMainPageResponse(tuple.getT1(), tuple.getT2()))
                );
    }

    private Mono<UserForm> createUserForm(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    UserForm userForm = new UserForm();
                    userForm.setLogin(user.getUsername());
                    userForm.setName(user.getName());
                    userForm.setBirthdate(user.getBirthday());
                    System.out.println(userForm.toString());
                    return userForm;
                });
    }

    private AccountForm convertToAccountForm(Account account) {
        AccountForm accountForm = new AccountForm();
        accountForm.setAccountId(Double.valueOf(account.getId()));
        accountForm.setUserName(account.getUserName());
        Currency currency = new Currency();
        currency.setName(account.getCurrency().name());
        currency.setTitle(account.getCurrency().getTitle());
        accountForm.setCurrency(currency);
        accountForm.setBalance(account.getBalance().doubleValue());
        accountForm.setIsExists(account.getIsExists());
        System.out.println(accountForm.toString());
        return accountForm;
    }


    private MainPageResponse createMainPageResponse(UserForm userForm, List<AccountForm> accountForms) {
        MainPageResponse response = new MainPageResponse();
        response.setUser(userForm);
        response.setAccounts(accountForms);
        return response;
    }

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
        return principal.getName();
    }
    public Mono<UserFormLogin> authUser(Mono<String> body){
        return body.flatMap(username -> userRepository.findByUsername(username).flatMap(user -> {
            UserFormLogin userFormLogin = new UserFormLogin();
            userFormLogin.setLogin(user.getUsername());
            userFormLogin.setPassword(user.getPassword());
            userFormLogin.setRole(user.getRole());
            return Mono.just(userFormLogin);
        }));
    }


}
