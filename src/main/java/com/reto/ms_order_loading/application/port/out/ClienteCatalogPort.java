package com.reto.ms_order_loading.application.port.out;

import java.util.Set;

public interface ClienteCatalogPort {

    Set<String> findActiveIds(Set<String> ids);
}
