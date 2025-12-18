package com.transferservice.service;

import com.transfer_service.generated.get.domain.TransferResponse;
import com.transfer_service.generated.post.api.DefaultApi;
import com.transfer_service.generated.post.domain.BlockerResponse;
import com.transfer_service.generated.post.domain.Transfer;
import com.transfer_service.generated.post.domain.TransferValue;
import com.transfer_service.generated.post.domain.Notification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApiPostService {
    private final DefaultApi defaultApi;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ApiPostService(DefaultApi defaultApi, KafkaTemplate<String, Object> kafkaTemplate) {
        this.defaultApi = defaultApi;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<TransferValue> getexchange(Mono<com.transfer_service.generated.get.domain.Transfer> transferFormMono, String userName) {
        return transferFormMono.flatMap(transferForm -> {
            Transfer transfer = new Transfer();
            transfer.setFromCurrency(transferForm.getFromCurrency());
            transfer.setToCurrency(transferForm.getToCurrency());
            transfer.setValue(transferForm.getValue());
            transfer.setToLogin(transferForm.getToLogin());
            return defaultApi.excangeservice(userName, transfer).flatMap(value -> {
                if (value.getSuccess()) {
                    return Mono.just(value);
                } else {
                    return Mono.empty();
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

    public Mono<Void> sendNotification(String username, String message) {
        try {
            Notification notification = new Notification();
            notification.setUsername(username);
            notification.setMessage(message);
            notification.setTimestamp(java.time.LocalDateTime.now().toString());
            kafkaTemplate.send("notifications.responses", username, notification);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Mono<BlockerResponse> blocker() {
        return defaultApi.apiBlockerGet();
    }
}