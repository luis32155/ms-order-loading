package com.reto.ms_order_loading.domain.model;

import java.time.LocalDate;

public record Pedido(
    String numeroPedido,
    String clienteId,
    String zonaId,
    LocalDate fechaEntrega,
    EstadoPedido estado,
    boolean requiereRefrigeracion
) {
}
