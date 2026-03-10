package com.reto.ms_order_loading.application.port.out;

import com.reto.ms_order_loading.domain.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyPort {

    Optional<IdempotencyRecord> findByKeyAndHash(String idempotencyKey, String hash);

    boolean createProcessing(String idempotencyKey, String hash);

    void markCompleted(String idempotencyKey, String hash, String payload);

    void markFailed(String idempotencyKey, String hash);
}
