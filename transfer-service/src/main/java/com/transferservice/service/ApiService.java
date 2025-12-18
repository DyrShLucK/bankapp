package com.transferservice.service;

import com.transfer_service.generated.get.domain.Transfer;
import com.transfer_service.generated.get.domain.TransferResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ApiService {
    private final ApiPostService apiPostService;

    public ApiService(ApiPostService apiPostService) {
        this.apiPostService = apiPostService;
    }

    public Mono<TransferResponse> getTransferResponse(Mono<Transfer> transfer, String username, String userName) {
        return apiPostService.blocker().flatMap(blockerResponse -> {
            if (!blockerResponse.getSuccess()) {
                TransferResponse errorResponse = new TransferResponse();
                errorResponse.setSuccess(false);
                errorResponse.setCause(List.of(blockerResponse.getCause()));
                return Mono.just(errorResponse);
            } else {
                return transfer.flatMap(transfer1 ->
                        apiPostService.getexchange(Mono.just(transfer1), userName)
                                .flatMap(transferValue -> {
                                    transfer1.setSummary(transferValue.getSummary());
                                    return apiPostService.toAccountService(Mono.just(transfer1), userName);
                                })
                                .flatMap(response -> {
                                    String transferType;
                                    if (transfer1.getToLogin() != null && transfer1.getToLogin().equals(username)) {
                                        transferType = "Перевод себе со счета на счет";
                                    } else if (transfer1.getToLogin() != null && !transfer1.getToLogin().isEmpty()) {
                                        transferType = "Перевод другому пользователю: " + transfer1.getToLogin();
                                    } else {
                                        transferType = "Перевод себе со счета на счет";
                                    }
                                    if (response.getSuccess()) {
                                        String message = "Перевод успешно выполнен: " + transfer1.getValue() + " " + transfer1.getFromCurrency() + " -> " + transfer1.getSummary() + transfer1.getToCurrency() + ". " + transferType;
                                        return apiPostService.sendNotification(username, message)
                                                .thenReturn(response)
                                                .onErrorResume(error -> {
                                                    System.err.println("Failed to send Kafka notification: " + error.getMessage());
                                                    return Mono.just(response);
                                                });
                                    } else {
                                        return Mono.just(response);
                                    }
                                })
                );
            }
        });
    }
}