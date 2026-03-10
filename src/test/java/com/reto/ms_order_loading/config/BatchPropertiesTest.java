package com.reto.ms_order_loading.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchPropertiesTest {

    @Test
    void shouldExposeConfiguredSize() {
        BatchProperties properties = new BatchProperties(500);

        assertThat(properties.size()).isEqualTo(500);
    }
}
