package com.reto.ms_order_loading.domain.model;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
