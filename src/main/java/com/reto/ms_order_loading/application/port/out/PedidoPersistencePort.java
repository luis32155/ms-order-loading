package com.reto.ms_order_loading.application.port.out;

import com.reto.ms_order_loading.domain.model.Pedido;

import java.util.List;
import java.util.Set;

public interface PedidoPersistencePort {

    Set<String> findExistingOrderNumbers(Set<String> orderNumbers);

    void saveAll(List<Pedido> pedidos);
}
