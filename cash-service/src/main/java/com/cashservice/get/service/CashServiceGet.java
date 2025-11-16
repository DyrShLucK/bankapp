package com.cashservice.get.service;

import com.cash_service.generated.get.domain.AccountCashResponse;
import com.cash_service.generated.get.domain.CashTransfer;
import com.cash_service.generated.post.domain.BlockerResponse;
import com.cashservice.post.service.CashServicePost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class CashServiceGet {

    private final CashServicePost cashServicePost;

    public CashServiceGet(CashServicePost cashServicePost) {
        this.cashServicePost = cashServicePost;
    }

    public Mono<AccountCashResponse> cashFunc(Mono<CashTransfer> cashTransfer, String username,  String sessionId) {
        return cashServicePost.getAccountserviceResponceCash(cashTransfer, username, sessionId);
    }
}
