package com.accountservice.service;

import com.account_service.domain.*;
import com.account_service.domain.Currency;
import com.accountservice.model.Account;
import com.accountservice.model.User;
import com.accountservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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
                        ).map(tuple -> {
                            MainPageResponse response = createMainPageResponse(tuple.getT1(), tuple.getT2());
                            response.setCurrencys(createCurrency());
                            return response;
                        })
                );
    }
    private List<Currency> createCurrency(){
        List<Currency> currencies = new ArrayList<>();

        for (com.accountservice.enums.Currency enumCurrency : com.accountservice.enums.Currency.values()) {
            Currency currency = new Currency();
            currency.setName(enumCurrency.name());
            currency.setTitle(enumCurrency.getTitle());
            currencies.add(currency);
        }

        return currencies;
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

    public Mono<UserFormLogin> authUser(Mono<String> body){
        return body.flatMap(username -> userRepository.findByUsername(username).flatMap(user -> {
            UserFormLogin userFormLogin = new UserFormLogin();
            userFormLogin.setLogin(user.getUsername());
            userFormLogin.setPassword(user.getPassword());
            userFormLogin.setRole(user.getRole());
            return Mono.just(userFormLogin);
        }));
    }
    public Mono<EditUserResponse> editUser(Mono<UpdateUserForm> userFormMono, String username) {
        System.out.println("Начало обработки запроса на редактирование пользователя: " + username);

        return userFormMono
                .flatMap(userForm -> processEditUserForm(userForm, username))
                .doOnSuccess(this::logEditResult);
    }

    private Mono<EditUserResponse> processEditUserForm(UpdateUserForm userForm, String username) {
        EditUserResponse response = new EditUserResponse();

        // Валидация даты рождения
        if (userForm.getBirthdate() != null) {
            try {
                LocalDate birthDate = userForm.getBirthdate();
                if (!isAdult(birthDate)) {
                    response.setSuccess(false);
                    response.setCause(List.of("Пользователю должно быть больше 18 лет"));
                    return Mono.just(response);
                }
            } catch (DateTimeParseException e) {
                response.setSuccess(false);
                response.setCause(List.of("Неверный формат даты рождения"));
                return Mono.just(response);
            }
        }

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new RuntimeException("Пользователь не найден")))
                .flatMap(user -> {
                    // Обновляем данные пользователя
                    if (userForm.getName() != null && !userForm.getName().trim().isEmpty()) {
                        user.setName(userForm.getName().trim());
                    }

                    if (userForm.getBirthdate() != null) {
                        try {
                            user.setBirthday(userForm.getBirthdate());
                        } catch (DateTimeParseException e) {
                            // Игнорируем ошибку парсинга, так как уже проверили выше
                        }
                    }

                    // Сохраняем обновленного пользователя
                    return userRepository.save(user)
                            .then(processAccounts(user.getUsername(), userForm.getAccounts()))
                            .map(result -> {
                                if (result.getSuccess()) {
                                    response.setSuccess(true);
                                } else {
                                    response.setSuccess(false);
                                    response.setCause(result.getCause());
                                }
                                return response;
                            });
                })
                .onErrorResume(e -> {
                    response.setSuccess(false);
                    response.setCause(List.of("Ошибка при обновлении пользователя: " + e.getMessage()));
                    return Mono.just(response);
                });
    }

    private Mono<EditUserResponse> processAccounts(String username, List<String> accountCurrencies) {
        EditUserResponse response = new EditUserResponse();

        if (accountCurrencies == null) {
            accountCurrencies = new ArrayList<>();
        }

        // Получаем все существующие аккаунты пользователя
        List<String> finalAccountCurrencies = accountCurrencies;
        return accountService.findOrCreateAccounts(username)
                .collectList()
                .flatMap(accounts -> {
                    List<Mono<Account>> accountUpdates = new ArrayList<>();
                    List<String> errorMessages = new ArrayList<>();

                    for (Account account : accounts) {
                        String currencyCode = account.getCurrency().name();
                        boolean shouldExist = finalAccountCurrencies.contains(currencyCode);

                        if (shouldExist) {
                            // Включаем аккаунт
                            if (!account.getIsExists()) {
                                account.setIsExists(true);
                                accountUpdates.add(accountService.saveAccount(account));
                            }
                        } else {
                            // Пытаемся отключить аккаунт
                            if (account.getIsExists()) {
                                if (account.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                                    account.setIsExists(false);
                                    accountUpdates.add(accountService.saveAccount(account));
                                } else {
                                    errorMessages.add("Невозможно отключить аккаунт " + currencyCode + ": на балансе есть средства");
                                }
                            }
                        }
                    }

                    if (!errorMessages.isEmpty()) {
                        response.setSuccess(false);
                        response.setCause(errorMessages);
                        return Mono.just(response);
                    }

                    // Выполняем все обновления аккаунтов
                    if (accountUpdates.isEmpty()) {
                        response.setSuccess(true);
                        return Mono.just(response);
                    }

                    return Flux.fromIterable(accountUpdates)
                            .flatMap(mono -> mono)
                            .then(Mono.fromCallable(() -> {
                                response.setSuccess(true);
                                return response;
                            }))
                            .onErrorResume(e -> {
                                response.setSuccess(false);
                                response.setCause(List.of("Ошибка при обновлении аккаунтов: " + e.getMessage()));
                                return Mono.just(response);
                            });
                });
    }

    private boolean isAdult(LocalDate birthDate) {
        return birthDate.isBefore(LocalDate.now().minusYears(18));
    }

    private void logEditResult(EditUserResponse response) {
        if (response != null) {
            System.out.println("Результат редактирования пользователя: успех=" + response.getSuccess());
            if (response.getCause() != null && !response.getCause().isEmpty()) {
                System.out.println("Причины ошибок: " + String.join(", ", response.getCause()));
            }
        }
    }

    public Mono<ResponseEntity<Void>> editPassword(Mono<PasswordChange> passwordChange, String username) {
        return passwordChange.flatMap(pass -> userRepository.findByUsername(username)
                .flatMap(user -> {
                    user.setPassword(pass.getPassword());

                    return userRepository.save(user)
                            .then(Mono.just(ResponseEntity.ok().<Void>build()));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().<Void>build())));
    }



    public Mono<AccountCashResponse> cash(Mono<CashTransfer> cashTransfer, String username) {
        return cashTransfer.flatMap(transferData -> {
            String currencyCode = transferData.getCurrencyTo();
            Double value = transferData.getValue();
            String action = transferData.getAction();

            AccountCashResponse initialValidationResponse = new AccountCashResponse();


            if (value == null || value <= 0) {
                initialValidationResponse.setSuccess(false);
                initialValidationResponse.setCause(List.of("Сумма должна быть положительным числом"));
                return Mono.just(initialValidationResponse);
            }

            return accountService.findOrCreateAccounts(username)
                    .filter(account -> currencyCode.equals(account.getCurrency().name()))
                    .next()
                    .flatMap(account -> {
                        AccountCashResponse response = new AccountCashResponse();

                        if (!account.getIsExists()) {
                            response.setSuccess(false);
                            response.setCause(List.of("Аккаунт с валютой " + currencyCode + " отключен"));
                            return Mono.just(response);
                        }

                        BigDecimal amount = BigDecimal.valueOf(value);

                        if ("PUT".equalsIgnoreCase(action)) {
                            account.setBalance(account.getBalance().add(amount));

                        } else if ("GET".equalsIgnoreCase(action)) {
                            if (account.getBalance().compareTo(amount) < 0) {
                                response.setSuccess(false);
                                response.setCause(List.of("Недостаточно денег для снятия. Баланс: " + account.getBalance() + ", Запрошено: " + amount));
                                return Mono.just(response);
                            } else {
                                account.setBalance(account.getBalance().subtract(amount));
                            }
                        }

                        return accountService.saveAccount(account)
                                .then(Mono.fromCallable(() -> {
                                    response.setSuccess(true);
                                    response.setCause(new ArrayList<>()); ;
                                    return response;
                                }))
                                .onErrorResume(throwable -> {
                                    System.err.println("Ошибка при сохранении аккаунта: " + throwable.getMessage());
                                    AccountCashResponse errorResponse = new AccountCashResponse();
                                    errorResponse.setSuccess(false);
                                    errorResponse.setCause(List.of("Ошибка при обновлении счета: " + throwable.getMessage()));
                                    return Mono.just(errorResponse);
                                });
                    }).switchIfEmpty(Mono.defer(() -> {
                        AccountCashResponse errorResponse = new AccountCashResponse();
                        errorResponse.setSuccess(false);
                        errorResponse.setCause(List.of("Аккаунт с валютой " + currencyCode + " не найден для пользователя " + username));
                        return Mono.just(errorResponse);
                    }));
        });
    }

}
