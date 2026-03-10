package com.reto.ms_order_loading.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.reto.ms_order_loading.adapter.out.persistence.entity.PedidoEntity;

import java.util.Collection;
import java.util.List;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, java.util.UUID> {

    @Query("select p.numeroPedido from PedidoEntity p where p.numeroPedido in :orderNumbers")
    List<String> findExistingOrderNumbers(Collection<String> orderNumbers);
}
