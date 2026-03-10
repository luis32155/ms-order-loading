package com.reto.ms_order_loading.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.reto.ms_order_loading.adapter.out.persistence.entity.CargaIdempotenciaEntity;
import com.reto.ms_order_loading.adapter.out.persistence.repository.CargaIdempotenciaJpaRepository;
import com.reto.ms_order_loading.application.port.out.IdempotencyPort;
import com.reto.ms_order_loading.domain.model.IdempotencyRecord;
import com.reto.ms_order_loading.domain.model.IdempotencyStatus;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyJpaAdapter implements IdempotencyPort {

    private final CargaIdempotenciaJpaRepository repository;


    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByKeyAndHash(String idempotencyKey, String hash) {
        return repository.findByIdempotencyKeyAndArchivoHash(idempotencyKey, hash)
            .map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createProcessing(String idempotencyKey, String hash) {
        try {
            CargaIdempotenciaEntity entity = new CargaIdempotenciaEntity();
            entity.setIdempotencyKey(idempotencyKey);
            entity.setArchivoHash(hash);
            entity.setStatus(IdempotencyStatus.PROCESSING.name());
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String idempotencyKey, String hash, String payload) {
        CargaIdempotenciaEntity entity = repository.findByIdempotencyKeyAndArchivoHash(idempotencyKey, hash)
            .orElseThrow();
        entity.setStatus(IdempotencyStatus.COMPLETED.name());
        entity.setResponsePayload(payload);
        repository.save(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String idempotencyKey, String hash) {
        repository.findByIdempotencyKeyAndArchivoHash(idempotencyKey, hash)
            .ifPresent(entity -> {
                entity.setStatus(IdempotencyStatus.FAILED.name());
                repository.save(entity);
            });
    }

    private IdempotencyRecord toDomain(CargaIdempotenciaEntity entity) {
        return new IdempotencyRecord(
            entity.getIdempotencyKey(),
            entity.getArchivoHash(),
            IdempotencyStatus.valueOf(entity.getStatus()),
            entity.getResponsePayload()
        );
    }
}
