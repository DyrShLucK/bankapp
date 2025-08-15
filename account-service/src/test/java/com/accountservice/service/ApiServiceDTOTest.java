package com.accountservice.service;

import com.account_service.generated.get.domain.*;
import com.account_service.generated.post.api.DefaultApi;
import com.account_service.generated.post.domain.Notification;
import com.accountservice.enums.Currency;
import com.accountservice.model.Account;
import com.accountservice.model.User;
import com.accountservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiServiceDTOTest {

    @Mock
    private AccountService accountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DefaultApi notificationApi;

    private ApiServiceDTO apiServiceDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apiServiceDTO = new ApiServiceDTO(accountService, userRepository, notificationApi);
    }

    @Test
    void testGetMainPageDTO() {
        String username = "testuser";

        User user = new User();
        user.setUsername(username);
        user.setName("Test User");
        user.setBirthday(LocalDate.now().minusYears(25));

        Account account = new Account();
        account.setId(1L);
        account.setUserName(username);
        account.setBalance(BigDecimal.valueOf(100));
        account.setIsExists(true);
        account.setCurrency(Currency.USD);

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(accountService.findOrCreateAccounts(username)).thenReturn(Flux.just(account));
        when(userRepository.findAll()).thenReturn(Flux.empty());

        when(notificationApi.apiNotificationsSetPost(any(Notification.class))).thenReturn(Mono.empty());

        Mono<MainPageResponse> result = apiServiceDTO.getMAinPageDTO(username);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getUser().getLogin().equals(username) &&
                                response.getAccounts().size() == 1)
                .verifyComplete();
    }

    @Test
    void testAuthUser() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setRole("USER");

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));

        Mono<UserFormLogin> result = apiServiceDTO.authUser(Mono.just(username));

        StepVerifier.create(result)
                .expectNextMatches(userForm ->
                        userForm.getLogin().equals(username) &&
                                userForm.getPassword().equals("password"))
                .verifyComplete();
    }

    @Test
    void testEditUserSuccess() {
        String username = "testuser";
        UpdateUserForm updateUserForm = new UpdateUserForm();
        updateUserForm.setName("New Name");
        updateUserForm.setBirthdate(LocalDate.now().minusYears(25));

        User user = new User();
        user.setUsername(username);
        user.setName("Old Name");
        user.setBirthday(LocalDate.now().minusYears(30));

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(accountService.findOrCreateAccounts(username)).thenReturn(Flux.empty());

        Mono<EditUserResponse> result = apiServiceDTO.editUser(Mono.just(updateUserForm), username);

        StepVerifier.create(result)
                .expectNextMatches(EditUserResponse::getSuccess)
                .verifyComplete();
    }

    @Test
    void testEditUserUnderage() {
        String username = "testuser";
        UpdateUserForm updateUserForm = new UpdateUserForm();
        updateUserForm.setBirthdate(LocalDate.now().minusYears(15)); // Меньше 18 лет

        Mono<EditUserResponse> result = apiServiceDTO.editUser(Mono.just(updateUserForm), username);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        !response.getSuccess() &&
                                response.getCause().get(0).contains("18 лет"))
                .verifyComplete();
    }

    @Test
    void testCashPutSuccess() {
        String username = "testuser";
        CashTransfer cashTransfer = new CashTransfer();
        cashTransfer.setCurrencyTo("USD");
        cashTransfer.setValue(100.0);
        cashTransfer.setAction("PUT");

        Account account = new Account();
        account.setCurrency(com.accountservice.enums.Currency.USD);
        account.setBalance(BigDecimal.valueOf(50));
        account.setIsExists(true);

        when(accountService.findOrCreateAccounts(username)).thenReturn(Flux.just(account));
        when(accountService.saveAccount(any(Account.class))).thenReturn(Mono.just(account));

        Mono<AccountCashResponse> result = apiServiceDTO.cash(Mono.just(cashTransfer), username);

        StepVerifier.create(result)
                .expectNextMatches(AccountCashResponse::getSuccess)
                .verifyComplete();
    }

    @Test
    void testCashGetInsufficientFunds() {
        String username = "testuser";
        CashTransfer cashTransfer = new CashTransfer();
        cashTransfer.setCurrencyTo("USD");
        cashTransfer.setValue(100.0);
        cashTransfer.setAction("GET");

        Account account = new Account();
        account.setCurrency(com.accountservice.enums.Currency.USD);
        account.setBalance(BigDecimal.valueOf(50));
        account.setIsExists(true);

        when(accountService.findOrCreateAccounts(username)).thenReturn(Flux.just(account));

        Mono<AccountCashResponse> result = apiServiceDTO.cash(Mono.just(cashTransfer), username);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        !response.getSuccess() &&
                                response.getCause().get(0).contains("Недостаточно денег"))
                .verifyComplete();
    }

    @Test
    void testTransferSuccess() {
        String username = "testuser";
        Transfer transfer = new Transfer();
        transfer.setFromCurrency("USD");
        transfer.setToCurrency("EUR");
        transfer.setValue(50.0);

        Account senderAccount = new Account();
        senderAccount.setCurrency(com.accountservice.enums.Currency.USD);
        senderAccount.setBalance(BigDecimal.valueOf(100));
        senderAccount.setIsExists(true);

        Account receiverAccount = new Account();
        receiverAccount.setCurrency(com.accountservice.enums.Currency.EUR);
        receiverAccount.setBalance(BigDecimal.valueOf(50));
        receiverAccount.setIsExists(true);

        when(accountService.findOrCreateAccounts(username))
                .thenReturn(Flux.just(senderAccount))
                .thenReturn(Flux.just(receiverAccount));
        when(accountService.saveAccount(any(Account.class)))
                .thenReturn(Mono.just(senderAccount))
                .thenReturn(Mono.just(receiverAccount));

        Mono<TransferResponse> result = apiServiceDTO.transfer(Mono.just(transfer), username);

        StepVerifier.create(result)
                .expectNextMatches(TransferResponse::getSuccess)
                .verifyComplete();
    }

    @Test
    void testTransferInvalidAmount() {
        String username = "testuser";
        Transfer transfer = new Transfer();
        transfer.setFromCurrency("USD");
        transfer.setToCurrency("EUR");
        transfer.setValue(-50.0);
        Mono<TransferResponse> result = apiServiceDTO.transfer(Mono.just(transfer), username);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        !response.getSuccess() &&
                                response.getCause().get(0).contains("положительным числом"))
                .verifyComplete();
    }
}