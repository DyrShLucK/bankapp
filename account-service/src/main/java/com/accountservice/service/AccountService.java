package com.accountservice.service;

import com.accountservice.enums.Currency;
import com.accountservice.model.Account;
import com.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Mono<Account> createAccount(String userName, Currency currency) {
        return accountRepository.findByUserNameAndCurrency(userName, currency).flatMap(account ->
            Mono.<Account>error(new IllegalArgumentException("Account with this currency already exists"))
        ).switchIfEmpty(Mono.defer(() -> {
            Account newAccount = new Account();
            newAccount.setUserName(userName);
            newAccount.setCurrency(currency);
            newAccount.setBalance(BigDecimal.ZERO);
            newAccount.setIsExists(false);
            return accountRepository.save(newAccount);
        }));
    }
    public Flux<Account> findOrCreateAccounts(String userName) {
        return accountRepository.findByUserName(userName)
                .sort(Comparator.comparing(account -> account.getCurrency().name()))
                .switchIfEmpty(createAccounts(userName));
    }
    public Flux<Account> createAccounts(String userName) {
        return Flux.fromIterable(List.of(Currency.values())).flatMap(currency -> createAccount(userName, currency));
    }
    public Flux<Account> findByUserName(String userName) {
        return accountRepository.findByUserName(userName);
    }
    public Mono<Account> findByUserNameAndCurrency(String userName, Currency currency) {
        return accountRepository.findByUserNameAndCurrency(userName, currency);
    }
    public void deleteAccount(String userName, Currency currency) {
        accountRepository.findByUserNameAndCurrency(userName, currency).flatMap(accountRepository::delete);
    }
    public Mono<Account> saveAccount(Account account) {
        return accountRepository.save(account);
    }
}
