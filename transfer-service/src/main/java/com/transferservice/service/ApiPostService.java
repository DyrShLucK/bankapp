package com.transferservice.service;

import com.transfer_service.generated.get.domain.TransferResponse;
import com.transfer_service.generated.post.api.DefaultApi;
import com.transfer_service.generated.post.domain.BlockerResponse;
import com.transfer_service.generated.post.domain.Notification;
import com.transfer_service.generated.post.domain.Transfer;
import com.transfer_service.generated.post.domain.TransferValue;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApiPostService {
    private final DefaultApi defaultApi;

    public ApiPostService(DefaultApi defaultApi) {
        this.defaultApi = defaultApi;
    }

    public Mono<TransferValue> getexchange(Mono<com.transfer_service.generated.get.domain.Transfer> transferFormMono, String userName) {
        return transferFormMono.flatMap(transferForm ->{
            Transfer transfer = new Transfer();
            transfer.setFromCurrency(transferForm.getFromCurrency());
            transfer.setToCurrency(transferForm.getToCurrency());
            transfer.setValue(transferForm.getValue());
            transfer.setToLogin(transferForm.getToLogin());
            return defaultApi.excangeservice(userName, transfer).flatMap(value -> {
                if (value.getSuccess()){
                    return Mono.just(value);
                }else {
                    return null;
                }
            });
        });
    }

    public Mono<TransferResponse> toAccountService(Mono<com.transfer_service.generated.get.domain.Transfer> transferFormMono, String userName) {
        return transferFormMono.flatMap(transfer -> {
            Transfer transfer2 = new Transfer();
            transfer2.setFromCurrency(transfer.getFromCurrency());
            transfer2.setToCurrency(transfer.getToCurrency());
            transfer2.setValue(transfer.getValue());
            transfer2.setToLogin(transfer.getToLogin());
            transfer2.setSummary(transfer.getSummary());
            return defaultApi.transferToAccountService(userName, transfer2).flatMap(transferResponse -> {
                TransferResponse transferResponse2 = new TransferResponse();
                transferResponse2.setSuccess(transferResponse.getSuccess());
                transferResponse2.setCause(transferResponse.getCause());
                return Mono.just(transferResponse2);
            });
        });
    }

    public Mono<Void> notification(Notification notification) {
        return defaultApi.apiNotificationsSetPost(notification);
    }

    public Mono<BlockerResponse> blocker() {
        return defaultApi.apiBlockerGet();
    }
}