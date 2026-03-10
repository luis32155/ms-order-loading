package com.reto.ms_order_loading.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import com.reto.ms_order_loading.adapter.in.rest.dto.CargaPedidosResponse;
import com.reto.ms_order_loading.application.port.in.CargarPedidosUseCase;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private CargarPedidosUseCase cargarPedidosUseCase;

    @Test
    void shouldDelegateToUseCase() {
        PedidoController controller = new PedidoController(cargarPedidosUseCase);
        MockMultipartFile file = new MockMultipartFile("file", "pedidos.csv", "text/csv", "data".getBytes());
        CargaPedidosResponse expected = new CargaPedidosResponse(1, 1, 0, List.of(), List.of());
        when(cargarPedidosUseCase.execute("key-1", file)).thenReturn(expected);

        var response = controller.cargarPedidos("key-1", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(cargarPedidosUseCase).execute("key-1", file);
    }
}
