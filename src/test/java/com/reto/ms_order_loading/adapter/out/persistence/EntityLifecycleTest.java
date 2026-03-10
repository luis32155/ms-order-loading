package com.reto.ms_order_loading.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.reto.ms_order_loading.adapter.out.persistence.entity.CargaIdempotenciaEntity;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ClienteEntity;
import com.reto.ms_order_loading.adapter.out.persistence.entity.PedidoEntity;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ZonaEntity;

class EntityLifecycleTest {

    @Test
    void shouldHandleClienteEntityProperties() {
        ClienteEntity entity = new ClienteEntity();
        entity.setId("CLI-123");
        entity.setActivo(true);

        assertThat(entity.getId()).isEqualTo("CLI-123");
        assertThat(entity.isActivo()).isTrue();
    }

    @Test
    void shouldHandleZonaEntityProperties() {
        ZonaEntity entity = new ZonaEntity();
        entity.setId("ZONA1");
        entity.setSoporteRefrigeracion(true);

        assertThat(entity.getId()).isEqualTo("ZONA1");
        assertThat(entity.isSoporteRefrigeracion()).isTrue();
    }

    @Test
    void shouldHandlePedidoEntityLifecycle() {
        PedidoEntity entity = new PedidoEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setNumeroPedido("P001");
        entity.setClienteId("CLI-123");
        entity.setZonaId("ZONA1");
        entity.setFechaEntrega(LocalDate.of(2026, 3, 20));
        entity.setEstado("PENDIENTE");
        entity.setRequiereRefrigeracion(true);

        entity.prePersist();
        LocalDateTime createdAt = entity.getCreatedAt();
        LocalDateTime oldUpdatedAt = entity.getUpdatedAt().minusMinutes(1);
        entity.setUpdatedAt(oldUpdatedAt);
        entity.preUpdate();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getNumeroPedido()).isEqualTo("P001");
        assertThat(entity.getClienteId()).isEqualTo("CLI-123");
        assertThat(entity.getZonaId()).isEqualTo("ZONA1");
        assertThat(entity.getFechaEntrega()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(entity.getEstado()).isEqualTo("PENDIENTE");
        assertThat(entity.isRequiereRefrigeracion()).isTrue();
        assertThat(createdAt).isNotNull();
        assertThat(entity.getUpdatedAt()).isAfter(oldUpdatedAt);
    }

    @Test
    void shouldHandleCargaIdempotenciaEntityLifecycle() {
        CargaIdempotenciaEntity entity = new CargaIdempotenciaEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setIdempotencyKey("key-1");
        entity.setArchivoHash("hash-1");
        entity.setStatus("PROCESSING");
        entity.setResponsePayload("payload");

        entity.prePersist();
        LocalDateTime oldUpdatedAt = entity.getUpdatedAt().minusMinutes(1);
        entity.setUpdatedAt(oldUpdatedAt);
        entity.preUpdate();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(entity.getArchivoHash()).isEqualTo("hash-1");
        assertThat(entity.getStatus()).isEqualTo("PROCESSING");
        assertThat(entity.getResponsePayload()).isEqualTo("payload");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isAfter(oldUpdatedAt);
    }
}
