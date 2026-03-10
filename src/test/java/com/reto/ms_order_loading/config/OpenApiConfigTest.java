package com.reto.ms_order_loading.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void shouldBuildOpenApiDefinition() {
        OpenApiConfig config = new OpenApiConfig();

        var openApi = config.pedidosOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Pedidos Batch Hexagonal API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getSecurity()).hasSize(1);
    }
}
