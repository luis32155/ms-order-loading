package com.reto.ms_order_loading.application.support;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import com.reto.ms_order_loading.application.exception.BadRequestException;
import com.reto.ms_order_loading.domain.model.CsvPedidoRow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class PedidoCsvReader {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        "numeroPedido",
        "clienteId",
        "fechaEntrega",
        "estado",
        "zonaEntrega",
        "requiereRefrigeracion"
    );

    public void readInBatches(byte[] content, int batchSize, Consumer<List<CsvPedidoRow>> batchConsumer) {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
             CSVParser parser = csvFormat.parse(reader)) {
            validateHeaders(parser.getHeaderMap().keySet());
            List<CsvPedidoRow> batch = new ArrayList<>(batchSize);
            for (CSVRecord record : parser) {
                int lineNumber = Math.toIntExact(record.getRecordNumber()) + 1;
                batch.add(new CsvPedidoRow(
                    lineNumber,
                    record.get("numeroPedido"),
                    record.get("clienteId"),
                    record.get("fechaEntrega"),
                    record.get("estado"),
                    record.get("zonaEntrega"),
                    record.get("requiereRefrigeracion")
                ));
                if (batch.size() == batchSize) {
                    batchConsumer.accept(List.copyOf(batch));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                batchConsumer.accept(List.copyOf(batch));
            }
        } catch (IOException exception) {
            throw new BadRequestException("No se pudo leer el archivo CSV", List.of(exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El archivo CSV no tiene el formato esperado", List.of(exception.getMessage()));
        }
    }

    private void validateHeaders(Set<String> headers) {
        if (!headers.containsAll(REQUIRED_HEADERS) || headers.size() != REQUIRED_HEADERS.size()) {
            throw new IllegalArgumentException("Las columnas válidas son: numeroPedido,clienteId,fechaEntrega,estado,zonaEntrega,requiereRefrigeracion");
        }
    }
}
