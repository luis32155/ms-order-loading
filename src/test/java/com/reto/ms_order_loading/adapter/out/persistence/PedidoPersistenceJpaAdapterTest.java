package com.reto.ms_order_loading.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.reto.ms_order_loading.adapter.out.persistence.entity.PedidoEntity;
import com.reto.ms_order_loading.adapter.out.persistence.mapper.PedidoEntityMapper;
import com.reto.ms_order_loading.adapter.out.persistence.repository.PedidoJpaRepository;
import com.reto.ms_order_loading.domain.model.EstadoPedido;
import com.reto.ms_order_loading.domain.model.Pedido;

@ExtendWith(MockitoExtension.class)
class PedidoPersistenceJpaAdapterTest {

    @Mock
    private PedidoJpaRepository repository;

    @Mock
    private PedidoEntityMapper mapper;

    @Test
    void shouldReturnEmptySetWhenOrderNumbersAreEmpty() {
        PedidoPersistenceJpaAdapter adapter = new PedidoPersistenceJpaAdapter(repository, mapper);

        Set<String> result = adapter.findExistingOrderNumbers(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnExistingOrderNumbersInUpperCase() {
        PedidoPersistenceJpaAdapter adapter = new PedidoPersistenceJpaAdapter(repository, mapper);
        when(repository.findExistingOrderNumbers(Set.of("P001", "P002"))).thenReturn(List.of("p001", "P002"));

        Set<String> result = adapter.findExistingOrderNumbers(Set.of("P001", "P002"));

        assertThat(result).containsExactlyInAnyOrder("P001", "P002");
    }

    @Test
    void shouldMapAndSaveAllms_order_loading() {
        PedidoPersistenceJpaAdapter adapter = new PedidoPersistenceJpaAdapter(repository, mapper);
        Pedido pedido1 = new Pedido("P001", "CLI-123", "ZONA1", LocalDate.of(2026, 3, 20), EstadoPedido.PENDIENTE, true);
        Pedido pedido2 = new Pedido("P002", "CLI-999", "ZONA2", LocalDate.of(2026, 3, 21), EstadoPedido.CONFIRMADO, false);
        PedidoEntity entity1 = new PedidoEntity();
        entity1.setNumeroPedido("P001");
        PedidoEntity entity2 = new PedidoEntity();
        entity2.setNumeroPedido("P002");
        when(mapper.toEntity(pedido1)).thenReturn(entity1);
        when(mapper.toEntity(pedido2)).thenReturn(entity2);

        adapter.saveAll(List.of(pedido1, pedido2));

        verify(repository).saveAll(anyList());
        verify(repository).flush();
        verify(mapper).toEntity(pedido1);
        verify(mapper).toEntity(pedido2);
    }
}
