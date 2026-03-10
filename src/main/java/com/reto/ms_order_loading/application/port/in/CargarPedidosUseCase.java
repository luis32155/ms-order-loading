package com.reto.ms_order_loading.application.port.in;

import org.springframework.web.multipart.MultipartFile;
import com.reto.ms_order_loading.adapter.in.rest.dto.CargaPedidosResponse;

public interface CargarPedidosUseCase {

    CargaPedidosResponse execute(String idempotencyKey, MultipartFile file);
}
