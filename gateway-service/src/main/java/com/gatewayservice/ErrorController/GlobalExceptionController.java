package com.gatewayservice.ErrorController;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
@ControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler(IOException.class)
    public Mono<String> handleIOException(IOException ex, ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session -> {
                    session.getAttributes().put("error", "Ошибка чтения изображения: " + ex.getMessage());
                })
                .thenReturn("redirect:/admin/add-product");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<String> handleNoResourceFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("message", "Страница не найдена: " + ex.getMessage());
        model.addAttribute("statusCode", HttpStatus.NOT_FOUND.value());
        return Mono.just("error");
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    public Mono<String> handleMethodNotSupported(MethodNotAllowedException ex, Model model) {
        model.addAttribute("message", "Метод не поддерживается");
        model.addAttribute("statusCode", HttpStatus.METHOD_NOT_ALLOWED.value());
        return Mono.just("error");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<String> handleResponseStatusException(ResponseStatusException ex, Model model) {
        if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            model.addAttribute("message", "Данный сервис недоступен");
            model.addAttribute("statusCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        } else {
            model.addAttribute("message", "Ошибка: " + ex.getMessage());
            model.addAttribute("statusCode", ex.getStatusCode().value());
        }
        return Mono.just("error");
    }

    @ExceptionHandler(Exception.class)
    public Mono<String> handleGlobalError(Exception ex, Model model) {
        model.addAttribute("message", "Внутренняя ошибка сервера: " + ex.getMessage());
        model.addAttribute("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Mono.just("error");
    }
}