package com.reto.ms_order_loading.domain.model;

public record ValidationError(
    int lineNumber,
    ValidationErrorType type,
    String message
) {
}
