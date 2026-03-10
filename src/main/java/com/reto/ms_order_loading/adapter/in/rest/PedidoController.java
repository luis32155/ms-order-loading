package com.reto.ms_order_loading.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.reto.ms_order_loading.adapter.in.rest.dto.ApiErrorResponse;
import com.reto.ms_order_loading.adapter.in.rest.dto.CargaPedidosResponse;
import com.reto.ms_order_loading.application.port.in.CargarPedidosUseCase;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

    private final CargarPedidosUseCase cargarPedidosUseCase;

    @Operation(
        summary = "Cargar pedidos desde CSV",
        description = "Recibe un archivo CSV, valida cada fila, persiste solo los pedidos válidos y aplica idempotencia con Idempotency-Key + hash del archivo.",
        security = @SecurityRequirement(name = "bearerAuth"),
        parameters = {
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Llave de idempotencia de la carga")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Carga procesada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Carga duplicada en proceso", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        }
    )
    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CargaPedidosResponse> cargarPedidos(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(cargarPedidosUseCase.execute(idempotencyKey, file));
    }
}
