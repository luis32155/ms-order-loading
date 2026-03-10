package com.reto.ms_order_loading.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.reto.ms_order_loading.adapter.out.persistence.mapper.PedidoEntityMapper;
import com.reto.ms_order_loading.adapter.out.persistence.repository.PedidoJpaRepository;
import com.reto.ms_order_loading.application.port.out.PedidoPersistencePort;
import com.reto.ms_order_loading.domain.model.Pedido;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PedidoPersistenceJpaAdapter implements PedidoPersistencePort {

    private final PedidoJpaRepository pedidoJpaRepository;
    private final PedidoEntityMapper pedidoEntityMapper;

    @Override
    public Set<String> findExistingOrderNumbers(Set<String> orderNumbers) {
        if (orderNumbers.isEmpty()) {
            return Set.of();
        }
        return pedidoJpaRepository.findExistingOrderNumbers(orderNumbers).stream()
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void saveAll(List<Pedido> pedidos) {
        pedidoJpaRepository.saveAll(pedidos.stream().map(pedidoEntityMapper::toEntity).toList());
        pedidoJpaRepository.flush();
    }
}
