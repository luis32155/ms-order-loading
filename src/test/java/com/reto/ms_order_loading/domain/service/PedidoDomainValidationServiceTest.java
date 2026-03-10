package com.reto.ms_order_loading.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.reto.ms_order_loading.domain.model.CsvPedidoRow;
import com.reto.ms_order_loading.domain.model.EstadoPedido;
import com.reto.ms_order_loading.domain.model.RowValidationResult;
import com.reto.ms_order_loading.domain.model.ValidationContext;
import com.reto.ms_order_loading.domain.model.ValidationErrorType;
import com.reto.ms_order_loading.domain.model.Zona;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoDomainValidationServiceTest {

    private PedidoDomainValidationService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        service = new PedidoDomainValidationService();
        today = LocalDate.of(2026, 3, 10);
    }

    @Test
    void shouldValidateSuccessfullyWhenRowIsValid() {
        CsvPedidoRow row = new CsvPedidoRow(
                2,
                " p001 ",
                " cli-123 ",
                "2026-03-15",
                " pendiente ",
                " zona1 ",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.errors()).isEmpty();
        assertThat(result.pedido()).isPresent();
        assertThat(result.pedido().get().numeroPedido()).isEqualTo("P001");
        assertThat(result.pedido().get().clienteId()).isEqualTo("CLI-123");
        assertThat(result.pedido().get().zonaId()).isEqualTo("ZONA1");
        assertThat(result.pedido().get().fechaEntrega()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(result.pedido().get().estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(result.pedido().get().requiereRefrigeracion()).isTrue();
    }

    @Test
    void shouldReturnErrorWhenOrderNumberIsInvalid() {
        CsvPedidoRow row = new CsvPedidoRow(
                3,
                "P-001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.NUMERO_PEDIDO_INVALIDO);
    }

    @Test
    void shouldReturnErrorWhenOrderNumberIsDuplicatedInFile() {
        CsvPedidoRow row = new CsvPedidoRow(
                4,
                "P001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                true,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.DUPLICADO);
    }

    @Test
    void shouldReturnErrorWhenOrderNumberAlreadyExistsInDatabase() {
        CsvPedidoRow row = new CsvPedidoRow(
                5,
                "P001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of("P001"),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.DUPLICADO);
    }

    @Test
    void shouldReturnErrorWhenClientDoesNotExist() {
        CsvPedidoRow row = new CsvPedidoRow(
                6,
                "P001",
                "CLI-999",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.CLIENTE_NO_ENCONTRADO);
    }

    @Test
    void shouldReturnErrorWhenDateIsPast() {
        CsvPedidoRow row = new CsvPedidoRow(
                7,
                "P001",
                "CLI-123",
                "2026-03-09",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.FECHA_INVALIDA);
    }

    @Test
    void shouldReturnErrorWhenDateFormatIsInvalid() {
        CsvPedidoRow row = new CsvPedidoRow(
                8,
                "P001",
                "CLI-123",
                "10/03/2026",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.FECHA_INVALIDA);
    }

    @Test
    void shouldReturnErrorWhenStatusIsInvalid() {
        CsvPedidoRow row = new CsvPedidoRow(
                9,
                "P001",
                "CLI-123",
                "2026-03-15",
                "ANULADO",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.ESTADO_INVALIDO);
    }

    @Test
    void shouldReturnErrorWhenRefrigerationValueIsInvalid() {
        CsvPedidoRow row = new CsvPedidoRow(
                10,
                "P001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "SI"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.REFRIGERACION_INVALIDA);
    }

    @Test
    void shouldReturnErrorWhenZoneDoesNotExist() {
        CsvPedidoRow row = new CsvPedidoRow(
                11,
                "P001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA9",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.ZONA_INVALIDA);
    }

    @Test
    void shouldReturnErrorWhenZoneDoesNotSupportColdChain() {
        CsvPedidoRow row = new CsvPedidoRow(
                12,
                "P001",
                "CLI-123",
                "2026-03-15",
                "PENDIENTE",
                "ZONA1",
                "true"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", false)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).type()).isEqualTo(ValidationErrorType.CADENA_FRIO_NO_SOPORTADA);
    }

    @Test
    void shouldReturnMultipleErrorsWhenRowHasSeveralProblems() {
        CsvPedidoRow row = new CsvPedidoRow(
                13,
                "P-001",
                "CLI-999",
                "2026/03/01",
                "ANULADO",
                "ZONA9",
                "X"
        );

        ValidationContext context = new ValidationContext(
                false,
                Set.of(),
                Set.of("CLI-123"),
                Map.of("ZONA1", new Zona("ZONA1", true)),
                today
        );

        RowValidationResult result = service.validate(row, context);

        assertThat(result.pedido()).isEmpty();
        assertThat(result.errors()).hasSize(6);
        assertThat(result.errors())
                .extracting(error -> error.type().name())
                .containsExactlyInAnyOrder(
                        ValidationErrorType.NUMERO_PEDIDO_INVALIDO.name(),
                        ValidationErrorType.CLIENTE_NO_ENCONTRADO.name(),
                        ValidationErrorType.FECHA_INVALIDA.name(),
                        ValidationErrorType.ESTADO_INVALIDO.name(),
                        ValidationErrorType.REFRIGERACION_INVALIDA.name(),
                        ValidationErrorType.ZONA_INVALIDA.name()
                );
    }
}