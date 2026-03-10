package com.reto.ms_order_loading.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.reto.ms_order_loading.domain.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PedidoDomainValidationService {

    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    public RowValidationResult validate(CsvPedidoRow row, ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();

        String numeroPedido = normalize(row.numeroPedido());
        String clienteId = normalize(row.clienteId());
        String zonaEntrega = normalize(row.zonaEntrega());
        String estadoRaw = normalize(row.estado());
        String refrigeracionRaw = normalize(row.requiereRefrigeracion());

        if (!StringUtils.hasText(numeroPedido) || !ORDER_NUMBER_PATTERN.matcher(numeroPedido).matches()) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.NUMERO_PEDIDO_INVALIDO, "numeroPedido debe ser alfanumérico"));
        } else if (context.duplicateInFile() || context.existingOrderNumbers().contains(numeroPedido)) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.DUPLICADO, "numeroPedido ya existe en el archivo o en la base de datos"));
        }

        if (!StringUtils.hasText(clienteId) || !context.activeClientIds().contains(clienteId)) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.CLIENTE_NO_ENCONTRADO, "El cliente %s no existe o está inactivo".formatted(clienteId)));
        }

        LocalDate fechaEntrega = parseDate(row, errors, context.todayLima());
        EstadoPedido estado = parseStatus(row.lineNumber(), estadoRaw, errors);
        Boolean requiereRefrigeracion = parseBoolean(row.lineNumber(), refrigeracionRaw, errors);
        Zona zona = context.zonesById().get(zonaEntrega);

        if (!StringUtils.hasText(zonaEntrega) || zona == null) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.ZONA_INVALIDA, "La zona %s no existe".formatted(zonaEntrega)));
        }

        if (Boolean.TRUE.equals(requiereRefrigeracion) && zona != null && !zona.soporteRefrigeracion()) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.CADENA_FRIO_NO_SOPORTADA, "La zona %s no soporta cadena de frío".formatted(zonaEntrega)));
        }

        if (!errors.isEmpty()) {
            return new RowValidationResult(Optional.empty(), errors);
        }

        Pedido pedido = new Pedido(numeroPedido, clienteId, zonaEntrega, fechaEntrega, estado, requiereRefrigeracion);
        return new RowValidationResult(Optional.of(pedido), List.of());
    }

    private LocalDate parseDate(CsvPedidoRow row, List<ValidationError> errors, LocalDate todayLima) {
        try {
            LocalDate fecha = LocalDate.parse(normalize(row.fechaEntrega()));
            if (fecha.isBefore(todayLima)) {
                errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.FECHA_INVALIDA, "fechaEntrega no puede ser pasada"));
            }
            return fecha;
        } catch (DateTimeParseException exception) {
            errors.add(new ValidationError(row.lineNumber(), ValidationErrorType.FECHA_INVALIDA, "fechaEntrega no tiene un formato válido YYYY-MM-DD"));
            return null;
        }
    }

    private EstadoPedido parseStatus(int lineNumber, String status, List<ValidationError> errors) {
        try {
            return EstadoPedido.fromValue(status);
        } catch (IllegalArgumentException exception) {
            errors.add(new ValidationError(lineNumber, ValidationErrorType.ESTADO_INVALIDO, "estado debe ser PENDIENTE, CONFIRMADO o ENTREGADO"));
            return null;
        }
    }

    private Boolean parseBoolean(int lineNumber, String value, List<ValidationError> errors) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        errors.add(new ValidationError(lineNumber, ValidationErrorType.REFRIGERACION_INVALIDA, "requiereRefrigeracion debe ser true o false"));
        return null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
