package com.reto.ms_order_loading.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ClienteEntity;
import com.reto.ms_order_loading.adapter.out.persistence.repository.ClienteJpaRepository;

@ExtendWith(MockitoExtension.class)
class ClienteCatalogJpaAdapterTest {

    @Mock
    private ClienteJpaRepository repository;

    @Test
    void shouldReturnEmptySetWhenIdsAreEmpty() {
        ClienteCatalogJpaAdapter adapter = new ClienteCatalogJpaAdapter(repository);

        Set<String> result = adapter.findActiveIds(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnActiveIdsInUpperCase() {
        ClienteCatalogJpaAdapter adapter = new ClienteCatalogJpaAdapter(repository);
        ClienteEntity cliente1 = new ClienteEntity();
        cliente1.setId("cli-123");
        cliente1.setActivo(true);
        ClienteEntity cliente2 = new ClienteEntity();
        cliente2.setId("CLI-999");
        cliente2.setActivo(true);
        when(repository.findByIdInAndActivoTrue(Set.of("cli-123", "CLI-999"))).thenReturn(List.of(cliente1, cliente2));

        Set<String> result = adapter.findActiveIds(Set.of("cli-123", "CLI-999"));

        assertThat(result).containsExactlyInAnyOrder("CLI-123", "CLI-999");
    }
}
