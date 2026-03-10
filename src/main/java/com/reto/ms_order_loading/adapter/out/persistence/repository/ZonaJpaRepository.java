package com.reto.ms_order_loading.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.reto.ms_order_loading.adapter.out.persistence.entity.ZonaEntity;

import java.util.Collection;
import java.util.List;

public interface ZonaJpaRepository extends JpaRepository<ZonaEntity, String> {

    List<ZonaEntity> findByIdIn(Collection<String> ids);
}
