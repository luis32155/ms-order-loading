package com.reto.ms_order_loading.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.reto.ms_order_loading.adapter.in.rest.dto.CargaPedidosResponse;
import com.reto.ms_order_loading.adapter.in.rest.dto.ErrorAgrupadoResponse;
import com.reto.ms_order_loading.adapter.in.rest.dto.ErrorFilaResponse;
import com.reto.ms_order_loading.application.exception.BadRequestException;
import com.reto.ms_order_loading.application.exception.ConflictException;
import com.reto.ms_order_loading.application.port.in.CargarPedidosUseCase;
import com.reto.ms_order_loading.application.port.out.ClienteCatalogPort;
import com.reto.ms_order_loading.application.port.out.IdempotencyPort;
import com.reto.ms_order_loading.application.port.out.PedidoPersistencePort;
import com.reto.ms_order_loading.application.port.out.ZonaCatalogPort;
import com.reto.ms_order_loading.application.support.PedidoCsvReader;
import com.reto.ms_order_loading.application.support.Sha256Service;
import com.reto.ms_order_loading.config.BatchProperties;
import com.reto.ms_order_loading.domain.model.*;
import com.reto.ms_order_loading.domain.service.PedidoDomainValidationService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CargarPedidosService implements CargarPedidosUseCase {

    private final PedidoDomainValidationService validationService;
    private final PedidoPersistencePort pedidoPersistencePort;
    private final ClienteCatalogPort clienteCatalogPort;
    private final ZonaCatalogPort zonaCatalogPort;
    private final IdempotencyPort idempotencyPort;
    private final PedidoCsvReader pedidoCsvReader;
    private final Sha256Service sha256Service;
    private final BatchProperties batchProperties;
    private final Clock limaClock;
    private final ObjectMapper objectMapper;

    @Override
    public CargaPedidosResponse execute(String idempotencyKey, MultipartFile file) {
        validateRequest(idempotencyKey, file);

        byte[] content = readBytes(file);
        String hash = sha256Service.hash(content);

        return idempotencyPort.findByKeyAndHash(idempotencyKey, hash)
                .map(record -> resolveExistingResponse(record, idempotencyKey))
                .orElseGet(() -> createAndProcess(idempotencyKey, hash, content));
    }

    private CargaPedidosResponse createAndProcess(String idempotencyKey, String hash, byte[] content) {
        boolean created = idempotencyPort.createProcessing(idempotencyKey, hash);

        if (!created) {
            IdempotencyRecord existing = idempotencyPort.findByKeyAndHash(idempotencyKey, hash)
                    .orElseThrow(() -> new ConflictException("IDEMPOTENCY_CONFLICT", "La carga ya está siendo procesada", List.of(idempotencyKey)));
            return resolveExistingResponse(existing, idempotencyKey);
        }

        try {
            CargaPedidosResponse response = processFile(content);
            idempotencyPort.markCompleted(idempotencyKey, hash, writePayload(response));
            return response;
        } catch (RuntimeException exception) {
            idempotencyPort.markFailed(idempotencyKey, hash);
            throw exception;
        }
    }

    private CargaPedidosResponse processFile(byte[] content) {
        Set<String> seenOrderNumbers = new HashSet<>();
        LocalDate todayLima = LocalDate.now(limaClock);
        AtomicReference<ProcessingState> state = new AtomicReference<>(ProcessingState.empty());

        pedidoCsvReader.readInBatches(
                content,
                batchProperties.size(),
                batch -> state.updateAndGet(current -> current.merge(processBatch(batch, seenOrderNumbers, todayLima)))
        );

        return state.get().toResponse();
    }

    private BatchOutcome processBatch(List<CsvPedidoRow> batch, Set<String> seenOrderNumbers, LocalDate todayLima) {
        BatchReferenceData referenceData = loadReferenceData(batch, todayLima);

        BatchValidationResult validationResult = batch.stream()
                .map(row -> validateRow(row, seenOrderNumbers, referenceData))
                .reduce(BatchValidationResult.empty(), BatchValidationResult::merge, BatchValidationResult::merge);

        Optional.of(validationResult.pedidos())
                .filter(pedidos -> !pedidos.isEmpty())
                .ifPresent(pedidoPersistencePort::saveAll);

        return new BatchOutcome(batch.size(), validationResult.pedidos().size(), validationResult.errors());
    }

    private BatchReferenceData loadReferenceData(List<CsvPedidoRow> batch, LocalDate todayLima) {
        Set<String> orderNumbers = normalizeValues(batch.stream().map(CsvPedidoRow::numeroPedido));
        Set<String> clientIds = normalizeValues(batch.stream().map(CsvPedidoRow::clienteId));
        Set<String> zoneIds = normalizeValues(batch.stream().map(CsvPedidoRow::zonaEntrega));

        return new BatchReferenceData(
                clienteCatalogPort.findActiveIds(clientIds),
                zonaCatalogPort.findByIds(zoneIds),
                pedidoPersistencePort.findExistingOrderNumbers(orderNumbers),
                todayLima
        );
    }

    private BatchValidationResult validateRow(CsvPedidoRow row, Set<String> seenOrderNumbers, BatchReferenceData referenceData) {
        boolean duplicateInFile = isDuplicateInFile(row.numeroPedido(), seenOrderNumbers);
        ValidationContext context = new ValidationContext(
                duplicateInFile,
                referenceData.existingOrderNumbers(),
                referenceData.activeClientIds(),
                referenceData.zones(),
                referenceData.todayLima()
        );

        RowValidationResult result = validationService.validate(row, context);

        return new BatchValidationResult(
                result.pedido().stream().toList(),
                result.errors()
        );
    }

    private boolean isDuplicateInFile(String numeroPedido, Set<String> seenOrderNumbers) {
        String normalizedOrder = normalize(numeroPedido);
        return StringUtils.hasText(normalizedOrder) && !seenOrderNumbers.add(normalizedOrder);
    }

    private Set<String> normalizeValues(Stream<String> values) {
        return values
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .collect(Collectors.toSet());
    }

    private CargaPedidosResponse resolveExistingResponse(IdempotencyRecord record, String idempotencyKey) {
        if (record.status() == IdempotencyStatus.COMPLETED && StringUtils.hasText(record.responsePayload())) {
            try {
                return objectMapper.readValue(record.responsePayload(), CargaPedidosResponse.class);
            } catch (JsonProcessingException exception) {
                throw new ConflictException("IDEMPOTENCY_REPLAY_ERROR", "No se pudo reconstruir la respuesta idempotente", List.of(idempotencyKey));
            }
        }

        throw new ConflictException("IDEMPOTENCY_IN_PROGRESS", "La carga con la misma llave ya está en proceso", List.of(idempotencyKey));
    }

    private void validateRequest(String idempotencyKey, MultipartFile file) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BadRequestException("Idempotency-Key es obligatorio", List.of("Idempotency-Key"));
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio", List.of("file"));
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo leer el archivo recibido", List.of(exception.getMessage()));
        }
    }

    private String writePayload(CargaPedidosResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar la respuesta", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private record BatchReferenceData(
            Set<String> activeClientIds,
            Map<String, Zona> zones,
            Set<String> existingOrderNumbers,
            LocalDate todayLima
    ) {
    }

    private record BatchValidationResult(List<Pedido> pedidos, List<ValidationError> errors) {

        private static BatchValidationResult empty() {
            return new BatchValidationResult(List.of(), List.of());
        }

        private BatchValidationResult merge(BatchValidationResult other) {
            return new BatchValidationResult(
                    Stream.concat(pedidos.stream(), other.pedidos.stream()).toList(),
                    Stream.concat(errors.stream(), other.errors.stream()).toList()
            );
        }
    }

    private record BatchOutcome(int processed, int saved, List<ValidationError> errors) {
    }

    private record ProcessingState(int processed, int saved, List<ValidationError> errors) {

        private static ProcessingState empty() {
            return new ProcessingState(0, 0, List.of());
        }

        private ProcessingState merge(BatchOutcome outcome) {
            return new ProcessingState(
                    processed + outcome.processed(),
                    saved + outcome.saved(),
                    Stream.concat(errors.stream(), outcome.errors().stream()).toList()
            );
        }

        private CargaPedidosResponse toResponse() {
            List<ErrorFilaResponse> errorRows = errors.stream()
                    .map(error -> new ErrorFilaResponse(error.lineNumber(), error.type().name(), error.message()))
                    .toList();

            List<ErrorAgrupadoResponse> groupedErrors = errors.stream()
                    .collect(Collectors.groupingBy(error -> error.type().name(), Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> new ErrorAgrupadoResponse(entry.getKey(), entry.getValue()))
                    .sorted((left, right) -> left.tipo().compareToIgnoreCase(right.tipo()))
                    .toList();

            int rowsWithError = (int) errors.stream()
                    .mapToInt(ValidationError::lineNumber)
                    .distinct()
                    .count();

            return new CargaPedidosResponse(processed, saved, rowsWithError, errorRows, groupedErrors);
        }
    }
}
