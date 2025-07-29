package com.cashservice.post.service;

import com.cash_service.generated.get.domain.AccountCashResponse;
import com.cash_service.generated.get.domain.CashTransfer;
import com.cash_service.generated.post.api.DefaultApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CashServicePost {

    private final DefaultApi defaultApi;

    public CashServicePost(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }

    public Mono<AccountCashResponse>getAccountserviceResponceCash(Mono<CashTransfer> MonocashTransfer) {
        com.cash_service.generated.post.domain.CashTransfer cashTransferPost = new com.cash_service.generated.post.domain.CashTransfer();
        return MonocashTransfer.flatMap(cashTransfer -> {

            cashTransferPost.setAction(cashTransfer.getAction());
            cashTransferPost.setValue(cashTransfer.getValue());
            cashTransferPost.setCurrencyTo(cashTransfer.getCurrencyTo());

            return defaultApi.transferToAccountService(cashTransferPost).flatMap(accountCashResponsePost -> {

                AccountCashResponse accountCashResponseGet = new AccountCashResponse();
                accountCashResponseGet.setCause(accountCashResponsePost.getCause());
                accountCashResponseGet.setSuccess(accountCashResponsePost.getSuccess());

                return Mono.just(accountCashResponseGet);
            });
        });
    }
}
