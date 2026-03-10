package com.reto.ms_order_loading.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EstadoPedidoTest {

    @Test
    void shouldResolveValueIgnoringCase() {
        assertThat(EstadoPedido.fromValue("pendiente")).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(EstadoPedido.fromValue("CONFIRMADO")).isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(EstadoPedido.fromValue("Entregado")).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void shouldThrowWhenValueIsInvalid() {
        assertThatThrownBy(() -> EstadoPedido.fromValue("DESPACHADO"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Estado inválido");
    }
}
