package com.reto.ms_order_loading.application.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<String> details;

    public ApiException(String code, String message, HttpStatus status, List<String> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> details() {
        return details;
    }
}
