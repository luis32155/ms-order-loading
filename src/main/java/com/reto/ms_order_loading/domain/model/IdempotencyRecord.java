package com.reto.ms_order_loading.domain.model;

public record IdempotencyRecord(
    String idempotencyKey,
    String fileHash,
    IdempotencyStatus status,
    String responsePayload
) {
}
