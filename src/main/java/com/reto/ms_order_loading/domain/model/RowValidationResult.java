package com.reto.ms_order_loading.domain.model;

import java.util.List;
import java.util.Optional;

public record RowValidationResult(
    Optional<Pedido> pedido,
    List<ValidationError> errors
) {
}
