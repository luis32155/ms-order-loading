package com.reto.ms_order_loading.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sha256ServiceTest {

    private Sha256Service service;

    @BeforeEach
    void setUp() {
        service = new Sha256Service();
    }

    @Test
    void shouldGenerateSha256HashForText() {
        byte[] content = "hola".getBytes(StandardCharsets.UTF_8);

        String result = service.hash(content);

        assertThat(result).isEqualTo("b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79");
    }

    @Test
    void shouldGenerateSha256HashForEmptyContent() {
        byte[] content = new byte[0];

        String result = service.hash(content);

        assertThat(result).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void shouldAlwaysReturn64Characters() {
        byte[] content = "pedido-123".getBytes(StandardCharsets.UTF_8);

        String result = service.hash(content);

        assertThat(result).hasSize(64);
    }
}