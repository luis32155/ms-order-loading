package com.reto.ms_order_loading.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ClienteEntity;

import java.util.Collection;
import java.util.List;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, String> {

    List<ClienteEntity> findByIdInAndActivoTrue(Collection<String> ids);
}
