package com.reto.ms_order_loading.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.reto.ms_order_loading.adapter.out.persistence.repository.ZonaJpaRepository;
import com.reto.ms_order_loading.application.port.out.ZonaCatalogPort;
import com.reto.ms_order_loading.domain.model.Zona;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ZonaCatalogJpaAdapter implements ZonaCatalogPort {

    private final ZonaJpaRepository zonaJpaRepository;

    @Override
    public Map<String, Zona> findByIds(Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return zonaJpaRepository.findByIdIn(ids).stream()
            .collect(Collectors.toMap(item -> item.getId().toUpperCase(), item -> new Zona(item.getId().toUpperCase(), item.isSoporteRefrigeracion())));
    }
}
