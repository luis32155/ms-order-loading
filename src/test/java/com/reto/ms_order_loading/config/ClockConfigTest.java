package com.reto.ms_order_loading.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void shouldCreateClockInLimaZone() {
        ClockConfig config = new ClockConfig();

        var clock = config.limaClock();

        assertThat(clock.getZone().getId()).isEqualTo("America/Lima");
    }
}
