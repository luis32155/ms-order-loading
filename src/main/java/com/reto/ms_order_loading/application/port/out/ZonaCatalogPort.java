package com.reto.ms_order_loading.application.port.out;

import com.reto.ms_order_loading.domain.model.Zona;

import java.util.Map;
import java.util.Set;

public interface ZonaCatalogPort {

    Map<String, Zona> findByIds(Set<String> ids);
}
