package com.accountservice.controller;


import com.account_service.generated.get.api.DefaultApi;
import com.account_service.generated.get.domain.*;
import com.accountservice.repository.UserRepository;
import com.accountservice.service.SignupService;
import com.accountservice.service.ApiServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ApiController implements DefaultApi {
    @Autowired
    SignupService signupService;
    @Autowired
    private ApiServiceDTO toApiDTO;

    @Autowired
    private UserRepository userRepository;


    @Override
    public Mono<ResponseEntity<SignupResponse>> apiSignupPost(Mono<RegistrationForm> registrationForm, ServerWebExchange exchange) {
        return signupService.signup(registrationForm).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MainPageResponse>> apiGetMainPageGet(ServerWebExchange exchange) {
        return toApiDTO.getMAinPageDTO(exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserFormLogin>> apiGetUserPost(Mono<String> body, ServerWebExchange exchange) {
        return  toApiDTO.authUser(body).map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<EditUserResponse>> apiEditUserAccountsPost(Mono<UpdateUserForm> updateUserForm, ServerWebExchange exchange) {
        return toApiDTO.editUser(updateUserForm, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> apiEditPasswordPost(Mono<PasswordChange> passwordChange, ServerWebExchange exchange) {
        return toApiDTO.editPassword(passwordChange, exchange.getRequest().getHeaders().getFirst("X-User-Name"));
    }

    @Override
    public Mono<ResponseEntity<AccountCashResponse>> transferToAccountService(Mono<CashTransfer> cashTransfer, ServerWebExchange exchange) {
        return toApiDTO.cash(cashTransfer, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TransferResponse>> transferFromToAccountService(Mono<Transfer> transfer, ServerWebExchange exchange) {
        return toApiDTO.transfer(transfer, exchange.getRequest().getHeaders().getFirst("X-User-Name")).map(ResponseEntity::ok);
    }
}
