package com.reto.ms_order_loading.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.reto.ms_order_loading.adapter.out.persistence.entity.CargaIdempotenciaEntity;

import java.util.Optional;
import java.util.UUID;

public interface CargaIdempotenciaJpaRepository extends JpaRepository<CargaIdempotenciaEntity, UUID> {

    Optional<CargaIdempotenciaEntity> findByIdempotencyKeyAndArchivoHash(String idempotencyKey, String archivoHash);
}
