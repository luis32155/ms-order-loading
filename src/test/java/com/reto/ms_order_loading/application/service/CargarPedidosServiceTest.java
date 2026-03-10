package com.reto.ms_order_loading.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.reto.ms_order_loading.adapter.in.rest.dto.CargaPedidosResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.reto.ms_order_loading.application.exception.BadRequestException;
import com.reto.ms_order_loading.application.exception.ConflictException;
import com.reto.ms_order_loading.application.port.out.ClienteCatalogPort;
import com.reto.ms_order_loading.application.port.out.IdempotencyPort;
import com.reto.ms_order_loading.application.port.out.PedidoPersistencePort;
import com.reto.ms_order_loading.application.port.out.ZonaCatalogPort;
import com.reto.ms_order_loading.application.support.PedidoCsvReader;
import com.reto.ms_order_loading.application.support.Sha256Service;
import com.reto.ms_order_loading.config.BatchProperties;
import com.reto.ms_order_loading.domain.model.CsvPedidoRow;
import com.reto.ms_order_loading.domain.model.EstadoPedido;
import com.reto.ms_order_loading.domain.model.IdempotencyRecord;
import com.reto.ms_order_loading.domain.model.IdempotencyStatus;
import com.reto.ms_order_loading.domain.model.Pedido;
import com.reto.ms_order_loading.domain.model.RowValidationResult;
import com.reto.ms_order_loading.domain.model.ValidationError;
import com.reto.ms_order_loading.domain.model.ValidationErrorType;
import com.reto.ms_order_loading.domain.model.Zona;
import com.reto.ms_order_loading.domain.service.PedidoDomainValidationService;

@ExtendWith(MockitoExtension.class)
class CargarPedidosServiceTest {

    @Mock
    private PedidoDomainValidationService validationService;
    @Mock
    private PedidoPersistencePort pedidoPersistencePort;
    @Mock
    private ClienteCatalogPort clienteCatalogPort;
    @Mock
    private ZonaCatalogPort zonaCatalogPort;
    @Mock
    private IdempotencyPort idempotencyPort;
    @Mock
    private PedidoCsvReader pedidoCsvReader;
    @Mock
    private Sha256Service sha256Service;
    @Mock
    private ObjectMapper objectMapper;

    private CargarPedidosService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneId.of("America/Lima"));
        service = new CargarPedidosService(
            validationService,
            pedidoPersistencePort,
            clienteCatalogPort,
            zonaCatalogPort,
            idempotencyPort,
            pedidoCsvReader,
            sha256Service,
            new BatchProperties(500),
            clock,
            objectMapper
        );
    }

    @Test
    void shouldFailWhenIdempotencyKeyIsMissing() {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());

        assertThatThrownBy(() -> service.execute(" ", file))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Idempotency-Key es obligatorio");
    }

    @Test
    void shouldFailWhenFileIsMissing() {
        assertThatThrownBy(() -> service.execute("key-1", null))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("El archivo es obligatorio");
    }

    @Test
    void shouldFailWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("El archivo es obligatorio");
    }

    @Test
    void shouldFailWhenReadingMultipartFails() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("No se pudo leer el archivo recibido");
    }

    @Test
    void shouldReturnExistingCompletedResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        CargaPedidosResponse expected = new CargaPedidosResponse(2, 1, 1, List.of(), List.of());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1"))
            .thenReturn(Optional.of(new IdempotencyRecord("key-1", "hash-1", IdempotencyStatus.COMPLETED, "payload")));
        when(objectMapper.readValue("payload", CargaPedidosResponse.class)).thenReturn(expected);

        CargaPedidosResponse response = service.execute("key-1", file);

        assertThat(response).isEqualTo(expected);
        verify(idempotencyPort, never()).createProcessing(anyString(), anyString());
    }

    @Test
    void shouldThrowConflictWhenStoredPayloadCannotBeParsed() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1"))
            .thenReturn(Optional.of(new IdempotencyRecord("key-1", "hash-1", IdempotencyStatus.COMPLETED, "payload")));
        when(objectMapper.readValue("payload", CargaPedidosResponse.class)).thenThrow(new JsonProcessingException("bad") {});

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(ConflictException.class)
            .hasMessage("No se pudo reconstruir la respuesta idempotente");
    }

    @Test
    void shouldThrowConflictWhenExistingRecordIsProcessing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1"))
            .thenReturn(Optional.of(new IdempotencyRecord("key-1", "hash-1", IdempotencyStatus.PROCESSING, null)));

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(ConflictException.class)
            .hasMessage("La carga con la misma llave ya está en proceso");
    }

    @Test
    void shouldThrowConflictWhenCreateProcessingFailsAndRecordDoesNotExist() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1")).thenReturn(Optional.empty(), Optional.empty());
        when(idempotencyPort.createProcessing("key-1", "hash-1")).thenReturn(false);

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(ConflictException.class)
            .hasMessage("La carga ya está siendo procesada");
    }

    @Test
    void shouldReturnCompletedResponseWhenCreateProcessingFailsButRecordAlreadyCompleted() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        CargaPedidosResponse expected = new CargaPedidosResponse(1, 1, 0, List.of(), List.of());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1"))
            .thenReturn(Optional.empty(), Optional.of(new IdempotencyRecord("key-1", "hash-1", IdempotencyStatus.COMPLETED, "payload")));
        when(idempotencyPort.createProcessing("key-1", "hash-1")).thenReturn(false);
        when(objectMapper.readValue("payload", CargaPedidosResponse.class)).thenReturn(expected);

        CargaPedidosResponse response = service.execute("key-1", file);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void shouldProcessFileAndPersistValidOrders() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        CsvPedidoRow row1 = new CsvPedidoRow(2, "P001", "CLI-123", "2026-03-20", "PENDIENTE", "ZONA1", "true");
        CsvPedidoRow row2 = new CsvPedidoRow(3, "P002", "CLI-404", "2026-03-21", "PENDIENTE", "ZONA1", "false");
        Pedido pedido = new Pedido("P001", "CLI-123", "ZONA1", LocalDate.of(2026, 3, 20), EstadoPedido.PENDIENTE, true);
        ValidationError error = new ValidationError(3, ValidationErrorType.CLIENTE_NO_ENCONTRADO, "cliente inválido");

        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1")).thenReturn(Optional.empty());
        when(idempotencyPort.createProcessing("key-1", "hash-1")).thenReturn(true);
        doAnswer(invocation -> {
            Consumer<List<CsvPedidoRow>> consumer = invocation.getArgument(2);
            consumer.accept(List.of(row1, row2));
            return null;
        }).when(pedidoCsvReader).readInBatches(any(byte[].class), anyInt(), any());
        when(clienteCatalogPort.findActiveIds(Set.of("CLI-123", "CLI-404"))).thenReturn(Set.of("CLI-123"));
        when(zonaCatalogPort.findByIds(Set.of("ZONA1"))).thenReturn(Map.of("ZONA1", new Zona("ZONA1", true)));
        when(pedidoPersistencePort.findExistingOrderNumbers(Set.of("P001", "P002"))).thenReturn(Set.of());
        when(validationService.validate(eq(row1), any())).thenReturn(new RowValidationResult(Optional.of(pedido), List.of()));
        when(validationService.validate(eq(row2), any())).thenReturn(new RowValidationResult(Optional.empty(), List.of(error)));
        when(objectMapper.writeValueAsString(any(CargaPedidosResponse.class))).thenReturn("payload");

        CargaPedidosResponse response = service.execute("key-1", file);

        assertThat(response.totalProcesados()).isEqualTo(2);
        assertThat(response.guardados()).isEqualTo(1);
        assertThat(response.conError()).isEqualTo(1);
        assertThat(response.errores()).containsExactly(new com.reto.ms_order_loading.adapter.in.rest.dto.ErrorFilaResponse(3, "CLIENTE_NO_ENCONTRADO", "cliente inválido"));
        assertThat(response.erroresAgrupados()).containsExactly(new com.reto.ms_order_loading.adapter.in.rest.dto.ErrorAgrupadoResponse("CLIENTE_NO_ENCONTRADO", 1));
        verify(pedidoPersistencePort).saveAll(List.of(pedido));
        verify(idempotencyPort).markCompleted("key-1", "hash-1", "payload");
    }

    @Test
    void shouldMarkFailedWhenProcessingThrowsRuntimeException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1")).thenReturn(Optional.empty());
        when(idempotencyPort.createProcessing("key-1", "hash-1")).thenReturn(true);
        doThrow(new IllegalStateException("boom")).when(pedidoCsvReader).readInBatches(any(byte[].class), anyInt(), any());

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        verify(idempotencyPort).markFailed("key-1", "hash-1");
    }

    @Test
    void shouldMarkFailedWhenSerializationFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ms_order_loading.csv", "text/csv", "data".getBytes());
        CsvPedidoRow row1 = new CsvPedidoRow(2, "P001", "CLI-123", "2026-03-20", "PENDIENTE", "ZONA1", "true");
        Pedido pedido = new Pedido("P001", "CLI-123", "ZONA1", LocalDate.of(2026, 3, 20), EstadoPedido.PENDIENTE, true);

        when(sha256Service.hash(file.getBytes())).thenReturn("hash-1");
        when(idempotencyPort.findByKeyAndHash("key-1", "hash-1")).thenReturn(Optional.empty());
        when(idempotencyPort.createProcessing("key-1", "hash-1")).thenReturn(true);
        doAnswer(invocation -> {
            Consumer<List<CsvPedidoRow>> consumer = invocation.getArgument(2);
            consumer.accept(List.of(row1));
            return null;
        }).when(pedidoCsvReader).readInBatches(any(byte[].class), anyInt(), any());
        when(clienteCatalogPort.findActiveIds(Set.of("CLI-123"))).thenReturn(Set.of("CLI-123"));
        when(zonaCatalogPort.findByIds(Set.of("ZONA1"))).thenReturn(Map.of("ZONA1", new Zona("ZONA1", true)));
        when(pedidoPersistencePort.findExistingOrderNumbers(Set.of("P001"))).thenReturn(Set.of());
        when(validationService.validate(eq(row1), any())).thenReturn(new RowValidationResult(Optional.of(pedido), List.of()));
        when(objectMapper.writeValueAsString(any(CargaPedidosResponse.class))).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> service.execute("key-1", file))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No se pudo serializar la respuesta");

        verify(idempotencyPort).markFailed("key-1", "hash-1");
    }
}
