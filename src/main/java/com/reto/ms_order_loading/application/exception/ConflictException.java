package com.reto.ms_order_loading.application.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ConflictException extends ApiException {

    public ConflictException(String code, String message, List<String> details) {
        super(code, message, HttpStatus.CONFLICT, details);
    }
}
