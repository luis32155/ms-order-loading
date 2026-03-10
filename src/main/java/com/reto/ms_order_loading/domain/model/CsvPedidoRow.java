package com.reto.ms_order_loading.domain.model;

public record CsvPedidoRow(
    int lineNumber,
    String numeroPedido,
    String clienteId,
    String fechaEntrega,
    String estado,
    String zonaEntrega,
    String requiereRefrigeracion
) {
}
