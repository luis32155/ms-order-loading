package com.reto.ms_order_loading.adapter.in.rest;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import com.reto.ms_order_loading.adapter.in.rest.dto.ApiErrorResponse;
import com.reto.ms_order_loading.application.exception.ApiException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiErrorResponse(
                        exception.code(),
                        exception.getMessage(),
                        exception.details(),
                        MDC.get("correlationId")
                ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Falta un header requerido", List.of(exception.getHeaderName()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Falta una parte requerida en el multipart", List.of(exception.getRequestPartName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException exception) {
        return build(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "Conflicto de datos",
                List.of("El recurso ya existe o viola una restricción única")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error interno", List.of(exception.getMessage()));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, message, details, MDC.get("correlationId")));
    }
}