package com.transferservice.service;

import com.transfer_service.generated.get.domain.Transfer;
import com.transfer_service.generated.get.domain.TransferResponse;
import com.transfer_service.generated.post.ApiClient;
import com.transfer_service.generated.post.domain.Notification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ApiService {
    private final ApiPostService apiPostService;

    public ApiService(ApiPostService apiPostService) {
        this.apiPostService = apiPostService;
    }

    public Mono<TransferResponse> getTransferResponse(Mono<Transfer> transfer, String username) {
        System.out.println("get transfer response");
        return apiPostService.blocker().flatMap(blockerResponse -> {
            System.out.println("blocker response");
            if (!blockerResponse.getSuccess()) {
                TransferResponse errorResponse = new TransferResponse();
                errorResponse.setSuccess(false);
                errorResponse.setCause(List.of(blockerResponse.getCause()));
                return Mono.just(errorResponse);
            } else {
                return transfer.flatMap(transfer1 ->

                        apiPostService.getexchange(Mono.just(transfer1))
                                .flatMap(transferValue -> {
                                    System.out.println(transfer1);
                                    System.out.println(transferValue);
                                    transfer1.setSummary(transferValue.getSummary());
                                    return apiPostService.toAccountService(Mono.just(transfer1));
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
                                        Notification notification = new Notification();
                                        notification.setUsername(username);
                                        notification.setMessage("Перевод успешно выполнен: " + transfer1.getValue() + " " + transfer1.getFromCurrency() + " -> " + transfer1.getToCurrency() + ". " + transferType + transfer1.getSummary());
                                        notification.setTimestamp(java.time.LocalDateTime.now().toString());

                                        return apiPostService.notification(notification)
                                                .thenReturn(response)
                                                .onErrorResume(error -> {
                                                    System.err.println("Failed to send notification: " + error.getMessage());
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
