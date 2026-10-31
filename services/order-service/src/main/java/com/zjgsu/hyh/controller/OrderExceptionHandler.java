package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.service.OrderService.DependencyUnavailableException;
import com.zjgsu.hyh.service.OrderService.InvalidOrderException;
import com.zjgsu.hyh.service.OrderService.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail handleInvalidOrder(InvalidOrderException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ProblemDetail handleDependency(DependencyUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
