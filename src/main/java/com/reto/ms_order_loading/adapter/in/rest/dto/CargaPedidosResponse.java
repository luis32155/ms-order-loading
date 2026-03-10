package com.reto.ms_order_loading.adapter.in.rest.dto;

import java.util.List;

public record CargaPedidosResponse(
    int totalProcesados,
    int guardados,
    int conError,
    List<ErrorFilaResponse> errores,
    List<ErrorAgrupadoResponse> erroresAgrupados
) {
}
