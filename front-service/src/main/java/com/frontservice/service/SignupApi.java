package com.frontservice.service;

import com.frontUi.api.DefaultApi;
import com.frontUi.domain.*;
import com.frontservice.DTO.*;
import com.frontservice.DTO.RegistrationForm;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SignupApi {

    private final DefaultApi defaultApi;
    public SignupApi(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }
    public Mono<EditUserResponse> editAccountsAndUser(UserUpdateForm form){
        UpdateUserForm updateUserForm = new UpdateUserForm();
        updateUserForm.setName(form.getName());
        updateUserForm.setBirthdate(form.getBirthdate());
        updateUserForm.setAccounts(form.getAccount());
        return defaultApi.apiEditUserAccountsPost(updateUserForm);
    }
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public Mono<SignupResponse> signUp(RegistrationForm registrationForm ) {
        com.frontUi.domain.RegistrationForm form = new com.frontUi.domain.RegistrationForm();
        form.setName(registrationForm.getName());
        form.setPassword(passwordEncoder().encode(registrationForm.getPassword()));
        form.setConfirmPassword(passwordEncoder().encode(registrationForm.getPassword()));
        form.setBirthdate(registrationForm.getBirthdate());
        form.setLogin(registrationForm.getLogin());
        return  defaultApi.apiSignupPost(form);
    }
    public Mono<Void> editPassword(PasswordUdateForm form) {
        PasswordChange passwordChange = new PasswordChange();
        passwordChange.setPassword(passwordEncoder().encode(form.getPassword()));
        passwordChange.setConfirmPassword(passwordEncoder().encode(form.getConfirm_password()));
        return defaultApi.apiEditPasswordPost(passwordChange);
    }
    public Flux<Value> getExchange(){
        return defaultApi.apiExchangeGet();
    }
    public Mono<AccountCashResponse> cash(CashForm cashForm){
        CashTransfer cashTransfer = new CashTransfer();
        cashTransfer.setCurrencyTo(cashForm.getCurrency());
        cashTransfer.setValue(cashForm.getValue());
        cashTransfer.setAction(cashForm.getAction());
        return defaultApi.apiCashPost(cashTransfer);
    }
    public Mono<NotificationsGet> notification(){
        return defaultApi.apiNotificationsGet();
    }
    public Mono<TransferResponse> transfer(TransferForm transferForm){
        Transfer transfer = new Transfer();
        transfer.setValue(transferForm.getValue());
        transfer.setFromCurrency(transferForm.getFrom_currency());
        transfer.setToCurrency(transferForm.getTo_currency());
        transfer.setToLogin(transferForm.getTo_login());
        System.out.println("dtoTransfer");
        return defaultApi.apiTransferPost(transfer);
    }
}
