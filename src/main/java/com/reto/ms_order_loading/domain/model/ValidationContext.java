package com.reto.ms_order_loading.domain.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record ValidationContext(
    boolean duplicateInFile,
    Set<String> existingOrderNumbers,
    Set<String> activeClientIds,
    Map<String, Zona> zonesById,
    LocalDate todayLima
) {
}
