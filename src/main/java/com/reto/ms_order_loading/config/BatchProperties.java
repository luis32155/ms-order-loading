package com.reto.ms_order_loading.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.batch")
public record BatchProperties(
    @Min(500)
    @Max(1000)
    int size
) {
}
