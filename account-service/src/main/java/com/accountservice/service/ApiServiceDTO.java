package com.accountservice.service;
import com.account_service.generated.get.domain.*;

import com.account_service.generated.post.api.DefaultApi;
import com.accountservice.model.Notification;
import com.accountservice.model.Account;
import com.accountservice.model.User;
import com.accountservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


@Service
public class ApiServiceDTO {
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ApiServiceDTO(AccountService accountService, UserRepository userRepository, KafkaTemplate kafkaTemplate) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<MainPageResponse> getMAinPageDTO(String username) {
        return Mono.just(username)
                .flatMap(usernameStr ->
                        Mono.zip(
                                createUserForm(usernameStr),
                                accountService.findOrCreateAccounts(usernameStr)
                                        .map(this::convertToAccountForm)
                                        .collectList(),
                                getAllUsersExceptCurrent(usernameStr)
                        ).map(tuple -> {
                            MainPageResponse response = createMainPageResponse(tuple.getT1(), tuple.getT2());
                            response.setCurrencys(createCurrency());
                            response.setUsers(tuple.getT3());
                            return response;
                        })
                );
    }

    private Mono<List<Users>> getAllUsersExceptCurrent(String currentUsername) {
        return userRepository.findAll()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .map(this::convertToUsersDTO)
                .collectList();
    }

    private Users convertToUsersDTO(User user) {
        Users usersDTO = new Users();
        usersDTO.setLogin(user.getUsername());
        usersDTO.setName(user.getName());
        return usersDTO;
    }

    private List<com.account_service.generated.get.domain.Currency> createCurrency(){
        List<com.account_service.generated.get.domain.Currency> currencies = new ArrayList<>();
        for (com.accountservice.enums.Currency enumCurrency : com.accountservice.enums.Currency.values()) {
            com.account_service.generated.get.domain.Currency currency = new com.account_service.generated.get.domain.Currency();
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
                    return userForm;
                });
    }

    private AccountForm convertToAccountForm(Account account) {
        AccountForm accountForm = new AccountForm();
        accountForm.setAccountId(Double.valueOf(account.getId()));
        accountForm.setUserName(account.getUserName());
        com.account_service.generated.get.domain.Currency currency = new com.account_service.generated.get.domain.Currency();
        currency.setName(account.getCurrency().name());
        currency.setTitle(account.getCurrency().getTitle());
        accountForm.setCurrency(currency);
        accountForm.setBalance(account.getBalance().doubleValue());
        accountForm.setIsExists(account.getIsExists());
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
        return userFormMono
                .flatMap(userForm -> processEditUserForm(userForm, username))
                .doOnSuccess(response -> {
                    if (response != null) {
                        sendUserEditNotification(username, response.getSuccess(), response.getCause());
                    }
                });
    }

    private Mono<EditUserResponse> processEditUserForm(UpdateUserForm userForm, String username) {
        EditUserResponse response = new EditUserResponse();
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
                    boolean userChanged = false;
                    if (userForm.getName() != null && !userForm.getName().trim().isEmpty()) {
                        user.setName(userForm.getName().trim());
                        userChanged = true;
                    }
                    if (userForm.getBirthdate() != null) {
                        user.setBirthday(userForm.getBirthdate());
                        userChanged = true;
                    }
                    Mono<User> saveUserMono = userChanged ? userRepository.save(user) : Mono.just(user);

                    return saveUserMono
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
        List<String> finalAccountCurrencies = accountCurrencies;
        return accountService.findOrCreateAccounts(username)
                .collectList()
                .flatMap(accounts -> {
                    List<Mono<Account>> accountUpdates = new ArrayList<>();
                    List<String> errorMessages = new ArrayList<>();
                    for (Account account : accounts) {
                        String currencyCode = account.getCurrency().name();
                        boolean shouldExist = finalAccountCurrencies.contains(currencyCode);
                        boolean accountChanged = false;
                        if (shouldExist) {
                            if (!account.getIsExists()) {
                                account.setIsExists(true);
                                accountChanged = true;
                            }
                        } else {
                            if (account.getIsExists()) {
                                if (account.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                                    account.setIsExists(false);
                                    accountChanged = true;
                                } else {
                                    errorMessages.add("Невозможно отключить аккаунт " + currencyCode + ": на балансе есть средства");
                                }
                            }
                        }
                        if(accountChanged) {
                            accountUpdates.add(accountService.saveAccount(account));
                        }
                    }
                    if (!errorMessages.isEmpty()) {
                        response.setSuccess(false);
                        response.setCause(errorMessages);
                        return Mono.just(response);
                    }
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



    public Mono<ResponseEntity<Void>> editPassword(Mono<PasswordChange> passwordChange, String username) {
        return passwordChange.flatMap(pass -> userRepository.findByUsername(username)
                .flatMap(user -> {
                    user.setPassword(pass.getPassword());
                    return userRepository.save(user)
                            .then(Mono.just(true));
                })
                .switchIfEmpty(Mono.just(false))
                .flatMap(success -> {
                    ResponseEntity<Void> responseEntity = success ? ResponseEntity.ok().<Void>build() : ResponseEntity.notFound().<Void>build();
                    sendPasswordChangeNotification(username, success, success ? null : "Пользователь не найден");
                    return Mono.just(responseEntity);
                })
                .onErrorResume(throwable -> {
                    System.err.println("Ошибка при изменении пароля для пользователя " + username + ": " + throwable.getMessage());
                    sendPasswordChangeNotification(username, false, "Внутренняя ошибка сервера");
                    return Mono.just(ResponseEntity.status(500).<Void>build()); // Или другой код ошибки
                })
        );
    }

    private void sendUserEditNotification(String username, boolean success, List<String> causes) {
        try {
            String message;
            if (success) {
                message = "Данные пользователя успешно обновлены.";
            } else {
                message = "Ошибка при обновлении данных пользователя: " +
                        (causes != null && !causes.isEmpty() ? String.join(", ", causes) : "Неизвестная ошибка");
            }


            Notification notification = new Notification();
            notification.setUsername(username);
            notification.setMessage(message);
            notification.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            kafkaTemplate.send("notifications.responses", username, notification);

            System.out.println("User edit notification sent to Kafka topic 'notifications.responses' for user: " + username);

        } catch (Exception e) {
            System.err.println("Ошибка при подготовке/отправке уведомления об изменении пользователя для " + username + " в Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendPasswordChangeNotification(String username, boolean success, String errorMessage) {
        try {
            String message;
            if (success) {
                message = "Пароль успешно изменен.";
            } else {
                message = "Ошибка при изменении пароля: " + (errorMessage != null ? errorMessage : "Неизвестная ошибка");
            }

            Notification notification = new Notification();
            notification.setUsername(username);
            notification.setMessage(message);
            notification.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            kafkaTemplate.send("notifications.responses", username, notification);

            System.out.println("Password change notification sent to Kafka topic 'notifications.responses' for user: " + username);

        } catch (Exception e) {
            System.err.println("Ошибка при подготовке/отправке уведомления о смене пароля для " + username + " в Kafka: " + e.getMessage());
            e.printStackTrace();
        }
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
    public Mono<TransferResponse> transfer(Mono<Transfer> transferMono, String username) {
        return transferMono.flatMap(transfer -> {
            String fromCurrency = transfer.getFromCurrency();
            String toCurrency = transfer.getToCurrency();
            Double value = transfer.getValue();
            String toLogin = transfer.getToLogin();
            Double summary = transfer.getSummary();

            TransferResponse validationResponse = validateTransferInput(value, fromCurrency, toCurrency);
            if (!validationResponse.getSuccess()) {
                return Mono.just(validationResponse);
            }

            boolean isInternalTransfer = toLogin == null || toLogin.equals(username);
            String targetLogin = isInternalTransfer ? username : toLogin;

            if (isInternalTransfer && fromCurrency.equals(toCurrency)) {
                TransferResponse response = new TransferResponse();
                response.setSuccess(false);
                response.setCause(List.of("Выбран один и тот же счет"));
                return Mono.just(response);
            }

            BigDecimal transferAmount = BigDecimal.valueOf(value);
            BigDecimal summaryAmount = summary != null ? BigDecimal.valueOf(summary) : BigDecimal.ZERO;

            Mono<Account> senderAccountMono = accountService.findOrCreateAccounts(username)
                    .filter(account -> fromCurrency.equals(account.getCurrency().name()) && account.getIsExists())
                    .next();

            Mono<Account> receiverAccountMono = accountService.findOrCreateAccounts(targetLogin)
                    .filter(account -> toCurrency.equals(account.getCurrency().name()) && account.getIsExists())
                    .next();

            return Mono.zip(senderAccountMono, receiverAccountMono)
                    .flatMap(tuple -> {
                        Account senderAccount = tuple.getT1();
                        Account receiverAccount = tuple.getT2();

                        if (senderAccount.getId().equals(receiverAccount.getId())) {
                            TransferResponse response = new TransferResponse();
                            response.setSuccess(false);
                            response.setCause(List.of("Выбран один и тот же счет"));
                            return Mono.just(response);
                        }

                        if (senderAccount.getBalance().compareTo(transferAmount) < 0) {
                            TransferResponse response = new TransferResponse();
                            response.setSuccess(false);
                            response.setCause(List.of("Недостаточно средств на счете. Баланс: " + senderAccount.getBalance() + ", Требуется: " + transferAmount));
                            return Mono.just(response);
                        }

                        senderAccount.setBalance(senderAccount.getBalance().subtract(transferAmount));
                        receiverAccount.setBalance(receiverAccount.getBalance().add(summaryAmount));

                        return Mono.zip(
                                accountService.saveAccount(senderAccount),
                                accountService.saveAccount(receiverAccount)
                        ).then(Mono.fromCallable(() -> {
                            TransferResponse response = new TransferResponse();
                            response.setSuccess(true);
                            response.setCause(new ArrayList<>());
                            return response;
                        }));
                    })
                    .onErrorResume(throwable -> {
                        System.err.println("Ошибка при выполнении перевода: " + throwable.getMessage());
                        TransferResponse errorResponse = new TransferResponse();
                        errorResponse.setSuccess(false);
                        if (throwable.getMessage().contains("не найден")) {
                            errorResponse.setCause(List.of(throwable.getMessage()));
                        } else {
                            errorResponse.setCause(List.of("Ошибка при выполнении перевода: " + throwable.getMessage()));
                        }
                        return Mono.just(errorResponse);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        TransferResponse errorResponse = new TransferResponse();
                        errorResponse.setSuccess(false);
                        errorResponse.setCause(List.of("Один или оба счета не найдены или отключены"));
                        return Mono.just(errorResponse);
                    }));
        });
    }

    private TransferResponse validateTransferInput(Double value, String fromCurrency, String toCurrency) {
        TransferResponse response = new TransferResponse();

        if (value == null || value <= 0) {
            response.setSuccess(false);
            response.setCause(List.of("Сумма перевода должна быть положительным числом"));
            return response;
        }


        response.setSuccess(true);
        response.setCause(new ArrayList<>());
        return response;
    }


}
