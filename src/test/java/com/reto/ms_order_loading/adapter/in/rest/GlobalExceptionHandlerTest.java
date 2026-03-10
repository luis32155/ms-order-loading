package com.reto.ms_order_loading.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import com.reto.ms_order_loading.adapter.in.rest.dto.ApiErrorResponse;
import com.reto.ms_order_loading.application.exception.ApiException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldHandleApiException() {
        MDC.put("correlationId", "corr-1");
        ApiException exception = new ApiException("CODE", "mensaje", HttpStatus.CONFLICT, List.of("d1"));

        ResponseEntity<ApiErrorResponse> response = handler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("CODE", "mensaje", List.of("d1"), "corr-1"));
    }

    @Test
    void shouldHandleMissingHeader() throws Exception {
        MDC.put("correlationId", "corr-2");
        Method method = DummyController.class.getDeclaredMethod("endpoint", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MissingRequestHeaderException exception = new MissingRequestHeaderException("Idempotency-Key", parameter);

        ResponseEntity<ApiErrorResponse> response = handler.handleMissingHeader(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
            "BAD_REQUEST",
            "Falta un header requerido",
            List.of("Idempotency-Key"),
            "corr-2"
        ));
    }

    @Test
    void shouldHandleMissingPart() {
        MDC.put("correlationId", "corr-3");
        MissingServletRequestPartException exception = new MissingServletRequestPartException("file");

        ResponseEntity<ApiErrorResponse> response = handler.handleMissingPart(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
            "BAD_REQUEST",
            "Falta una parte requerida en el multipart",
            List.of("file"),
            "corr-3"
        ));
    }

    @Test
    void shouldHandleGenericException() {
        MDC.put("correlationId", "corr-4");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
            "INTERNAL_ERROR",
            "Ocurrió un error interno",
            List.of("boom"),
            "corr-4"
        ));
    }

    private static class DummyController {
        void endpoint(String header) {
        }
    }
}
