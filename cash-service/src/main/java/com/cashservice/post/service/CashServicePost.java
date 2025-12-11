package com.cashservice.post.service;

import com.cash_service.generated.get.domain.AccountCashResponse;
import com.cash_service.generated.get.domain.CashTransfer;
import com.cash_service.generated.post.api.DefaultApi;
import com.cash_service.generated.post.domain.Notification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CashServicePost {

    private final DefaultApi defaultApi;

    public CashServicePost(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }

    public Mono<AccountCashResponse> getAccountserviceResponceCash(Mono<CashTransfer> monoCashTransfer, String username, String userName) {
        com.cash_service.generated.post.domain.CashTransfer cashTransferPost = new com.cash_service.generated.post.domain.CashTransfer();

        return monoCashTransfer.flatMap(cashTransfer ->
                defaultApi.apiBlockerGet()
                        .flatMap(blockerResponse -> {
                            if (!blockerResponse.getSuccess()) {
                                AccountCashResponse accountCashResponseGet = new AccountCashResponse();
                                accountCashResponseGet.setCause(List.of(blockerResponse.getCause()));
                                accountCashResponseGet.setSuccess(blockerResponse.getSuccess());
                                return Mono.just(accountCashResponseGet);
                            } else {
                                cashTransferPost.setAction(cashTransfer.getAction());
                                cashTransferPost.setValue(cashTransfer.getValue());
                                cashTransferPost.setCurrencyTo(cashTransfer.getCurrencyTo());

                                return defaultApi.transferToAccountService(userName, cashTransferPost)
                                        .flatMap(accountCashResponsePost -> {
                                            AccountCashResponse accountCashResponseGet = new AccountCashResponse();
                                            accountCashResponseGet.setCause(accountCashResponsePost.getCause());
                                            accountCashResponseGet.setSuccess(accountCashResponsePost.getSuccess());

                                            Notification notification = createNotification(cashTransfer, accountCashResponsePost, username);
                                            return defaultApi.apiNotificationsSetPost(notification)
                                                    .then(Mono.just(accountCashResponseGet))
                                                    .onErrorResume(error -> {
                                                        System.err.println("Failed to send notification: " + error.getMessage());
                                                        return Mono.just(accountCashResponseGet);
                                                    });
                                        });
                            }
                        })
        );
    }

    private Notification createNotification(com.cash_service.generated.get.domain.CashTransfer cashTransfer,
                                            com.cash_service.generated.post.domain.AccountCashResponse response, String username) {
        Notification notification = new Notification();
        notification.setUsername(username);
        notification.setMessage(createMessage(cashTransfer, response));
        notification.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return notification;
    }
    private String createMessage(com.cash_service.generated.get.domain.CashTransfer cashTransfer,
                                 com.cash_service.generated.post.domain.AccountCashResponse response) {
        String actionText = getActionText(cashTransfer.getAction());
        String currency = cashTransfer.getCurrencyTo() != null ? cashTransfer.getCurrencyTo() : "не указана";

        if (response.getSuccess()) {
            return String.format("Операция \"%s\" на сумму %.2f %s успешно выполнена",
                    actionText, cashTransfer.getValue(), currency);
        } else {
            return String.format("Операция \"%s\" на сумму %.2f %s не удалась: %s",
                    actionText, cashTransfer.getValue(), currency,
                    String.join(", ", response.getCause()));
        }
    }

    private String getActionText(String action) {
        if (action == null) {
            return "Неизвестная операция";
        }

        switch (action.toLowerCase()) {
            case "put":
                return "Пополнение счета";
            case "get":
                return "Снятие со счета";
            default:
                return action;
        }
    }
}
