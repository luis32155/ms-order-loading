package com.reto.ms_order_loading.adapter.in.rest.dto;

public record ErrorFilaResponse(
    int linea,
    String tipo,
    String mensaje
) {
}
