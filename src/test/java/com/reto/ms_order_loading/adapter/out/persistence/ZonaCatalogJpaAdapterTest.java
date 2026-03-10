package com.reto.ms_order_loading.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ZonaEntity;
import com.reto.ms_order_loading.adapter.out.persistence.repository.ZonaJpaRepository;
import com.reto.ms_order_loading.domain.model.Zona;

@ExtendWith(MockitoExtension.class)
class ZonaCatalogJpaAdapterTest {

    @Mock
    private ZonaJpaRepository repository;

    @Test
    void shouldReturnEmptyMapWhenIdsAreEmpty() {
        ZonaCatalogJpaAdapter adapter = new ZonaCatalogJpaAdapter(repository);

        Map<String, Zona> result = adapter.findByIds(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldMapZonesInUpperCase() {
        ZonaCatalogJpaAdapter adapter = new ZonaCatalogJpaAdapter(repository);
        ZonaEntity zona1 = new ZonaEntity();
        zona1.setId("zona1");
        zona1.setSoporteRefrigeracion(true);
        ZonaEntity zona2 = new ZonaEntity();
        zona2.setId("ZONA2");
        zona2.setSoporteRefrigeracion(false);
        when(repository.findByIdIn(Set.of("zona1", "ZONA2"))).thenReturn(List.of(zona1, zona2));

        Map<String, Zona> result = adapter.findByIds(Set.of("zona1", "ZONA2"));

        assertThat(result).containsEntry("ZONA1", new Zona("ZONA1", true));
        assertThat(result).containsEntry("ZONA2", new Zona("ZONA2", false));
    }
}
