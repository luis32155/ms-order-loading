package com.reto.ms_order_loading.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.reto.ms_order_loading.adapter.out.persistence.repository.ClienteJpaRepository;
import com.reto.ms_order_loading.application.port.out.ClienteCatalogPort;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClienteCatalogJpaAdapter implements ClienteCatalogPort {

    private final ClienteJpaRepository clienteJpaRepository;

    @Override
    public Set<String> findActiveIds(Set<String> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return clienteJpaRepository.findByIdInAndActivoTrue(ids).stream()
            .map(item -> item.getId().toUpperCase())
            .collect(Collectors.toSet());
    }
}
