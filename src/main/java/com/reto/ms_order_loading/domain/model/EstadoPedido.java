package com.reto.ms_order_loading.domain.model;

import java.util.Arrays;

public enum EstadoPedido {
    PENDIENTE,
    CONFIRMADO,
    ENTREGADO;

    public static EstadoPedido fromValue(String value) {
        return Arrays.stream(values())
            .filter(item -> item.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Estado inválido"));
    }
}
