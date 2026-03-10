package com.reto.ms_order_loading.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.reto.ms_order_loading.adapter.out.persistence.entity.PedidoEntity;
import com.reto.ms_order_loading.domain.model.Pedido;

@Mapper(componentModel = "spring")
public interface PedidoEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", expression = "java(pedido.estado().name())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PedidoEntity toEntity(Pedido pedido);
}