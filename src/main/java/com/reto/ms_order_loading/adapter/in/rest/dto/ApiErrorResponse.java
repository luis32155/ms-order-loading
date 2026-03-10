package com.reto.ms_order_loading.adapter.in.rest.dto;

import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<String> details,
    String correlationId
) {
}
