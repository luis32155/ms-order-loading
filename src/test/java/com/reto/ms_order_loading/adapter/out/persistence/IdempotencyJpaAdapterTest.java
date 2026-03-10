package com.reto.ms_order_loading.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import com.reto.ms_order_loading.adapter.out.persistence.entity.CargaIdempotenciaEntity;
import com.reto.ms_order_loading.adapter.out.persistence.repository.CargaIdempotenciaJpaRepository;
import com.reto.ms_order_loading.domain.model.IdempotencyStatus;

@ExtendWith(MockitoExtension.class)
class IdempotencyJpaAdapterTest {

    @Mock
    private CargaIdempotenciaJpaRepository repository;

    @Test
    void shouldFindByKeyAndHash() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        CargaIdempotenciaEntity entity = new CargaIdempotenciaEntity();
        entity.setIdempotencyKey("key-1");
        entity.setArchivoHash("hash-1");
        entity.setStatus("COMPLETED");
        entity.setResponsePayload("{\"ok\":true}");
        when(repository.findByIdempotencyKeyAndArchivoHash("key-1", "hash-1")).thenReturn(Optional.of(entity));

        var result = adapter.findByKeyAndHash("key-1", "hash-1");

        assertThat(result).isPresent();
        assertThat(result.get().idempotencyKey()).isEqualTo("key-1");
        assertThat(result.get().fileHash()).isEqualTo("hash-1");
        assertThat(result.get().status()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(result.get().responsePayload()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void shouldCreateProcessingRecord() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);

        boolean result = adapter.createProcessing("key-1", "hash-1");

        assertThat(result).isTrue();
        ArgumentCaptor<CargaIdempotenciaEntity> captor = ArgumentCaptor.forClass(CargaIdempotenciaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("key-1");
        assertThat(captor.getValue().getArchivoHash()).isEqualTo("hash-1");
        assertThat(captor.getValue().getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void shouldReturnFalseWhenConstraintViolationOccurs() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        when(repository.saveAndFlush(any(CargaIdempotenciaEntity.class))).thenThrow(new DataIntegrityViolationException("dup"));

        boolean result = adapter.createProcessing("key-1", "hash-1");

        assertThat(result).isFalse();
    }

    @Test
    void shouldMarkCompleted() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        CargaIdempotenciaEntity entity = new CargaIdempotenciaEntity();
        entity.setStatus("PROCESSING");
        when(repository.findByIdempotencyKeyAndArchivoHash("key-1", "hash-1")).thenReturn(Optional.of(entity));

        adapter.markCompleted("key-1", "hash-1", "{\"ok\":true}");

        assertThat(entity.getStatus()).isEqualTo("COMPLETED");
        assertThat(entity.getResponsePayload()).isEqualTo("{\"ok\":true}");
        verify(repository).save(entity);
    }

    @Test
    void shouldThrowWhenMarkCompletedDoesNotFindRecord() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        when(repository.findByIdempotencyKeyAndArchivoHash("key-1", "hash-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.markCompleted("key-1", "hash-1", "payload"))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void shouldMarkFailedWhenRecordExists() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        CargaIdempotenciaEntity entity = new CargaIdempotenciaEntity();
        entity.setStatus("PROCESSING");
        when(repository.findByIdempotencyKeyAndArchivoHash("key-1", "hash-1")).thenReturn(Optional.of(entity));

        adapter.markFailed("key-1", "hash-1");

        assertThat(entity.getStatus()).isEqualTo("FAILED");
        verify(repository).save(entity);
    }

    @Test
    void shouldDoNothingWhenMarkFailedDoesNotFindRecord() {
        IdempotencyJpaAdapter adapter = new IdempotencyJpaAdapter(repository);
        when(repository.findByIdempotencyKeyAndArchivoHash("key-1", "hash-1")).thenReturn(Optional.empty());

        adapter.markFailed("key-1", "hash-1");

        verify(repository, never()).save(any(CargaIdempotenciaEntity.class));
    }
}
