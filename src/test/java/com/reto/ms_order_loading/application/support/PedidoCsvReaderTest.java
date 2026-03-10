package com.reto.ms_order_loading.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.reto.ms_order_loading.application.exception.BadRequestException;
import com.reto.ms_order_loading.domain.model.CsvPedidoRow;

class PedidoCsvReaderTest {

    private final PedidoCsvReader reader = new PedidoCsvReader();

    @Test
    void shouldReadCsvInBatches() {
        String csv = String.join("\n",
            "numeroPedido,clienteId,fechaEntrega,estado,zonaEntrega,requiereRefrigeracion",
            "P001,CLI-123,2026-03-20,PENDIENTE,ZONA1,true",
            "P002,CLI-999,2026-03-21,CONFIRMADO,ZONA2,false",
            "P003,CLI-123,2026-03-22,ENTREGADO,ZONA3,true"
        );

        List<List<CsvPedidoRow>> batches = new ArrayList<>();

        reader.readInBatches(csv.getBytes(StandardCharsets.UTF_8), 2, batches::add);

        assertThat(batches).hasSize(2);
        assertThat(batches.get(0)).hasSize(2);
        assertThat(batches.get(1)).hasSize(1);
        assertThat(batches.get(0).get(0).lineNumber()).isEqualTo(2);
        assertThat(batches.get(0).get(0).numeroPedido()).isEqualTo("P001");
        assertThat(batches.get(1).get(0).lineNumber()).isEqualTo(4);
        assertThat(batches.get(1).get(0).numeroPedido()).isEqualTo("P003");
    }

    @Test
    void shouldThrowWhenHeadersAreInvalid() {
        String csv = String.join("\n",
            "numeroPedido,clienteId,fechaEntrega,estado,zonaEntrega",
            "P001,CLI-123,2026-03-20,PENDIENTE,ZONA1"
        );

        assertThatThrownBy(() -> reader.readInBatches(csv.getBytes(StandardCharsets.UTF_8), 2, batch -> {}))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("El archivo CSV no tiene el formato esperado");
    }
}
