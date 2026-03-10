package com.reto.ms_order_loading.application.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class BadRequestException extends ApiException {

    public BadRequestException(String message, List<String> details) {
        super("BAD_REQUEST", message, HttpStatus.BAD_REQUEST, details);
    }
}
