package com.accountservice.repository;

import com.accountservice.enums.Currency;
import com.accountservice.model.Account;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.CrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends R2dbcRepository<Account, Long> {
    Mono<Account> findByUserNameAndCurrency(String username, Currency currency);
    Flux<Account> findByUserName(String username);
}
