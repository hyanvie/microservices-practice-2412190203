package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.service.ItemService.ItemNotFoundException;
import com.zjgsu.hyh.service.ItemService.ItemUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ItemExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ProblemDetail handleNotFound(ItemNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ItemUnavailableException.class)
    public ProblemDetail handleUnavailable(ItemUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
