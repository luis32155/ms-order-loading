package com.reto.ms_order_loading.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "security.jwt.secret=12345678901234567890123456789012"
})
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private SecurityConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldLoadSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void shouldBuildJwtDecoder() {
        var decoder = config.jwtDecoder("12345678901234567890123456789012");
        assertThat(decoder).isNotNull();
    }

    @Test
    void shouldWriteUnauthorizedResponse() throws Exception {
        MDC.put("correlationId", "corr-1");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        config.authenticationEntryPoint(objectMapper)
                .commence(
                        new org.springframework.mock.web.MockHttpServletRequest(),
                        response,
                        new org.springframework.security.authentication.BadCredentialsException("bad credentials")
                );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(response.getContentAsString()).contains("corr-1");
    }

    @Test
    void shouldWriteForbiddenResponse() throws Exception {
        MDC.put("correlationId", "corr-2");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        config.accessDeniedHandler(objectMapper)
                .handle(
                        new org.springframework.mock.web.MockHttpServletRequest(),
                        response,
                        new org.springframework.security.access.AccessDeniedException("forbidden")
                );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("FORBIDDEN");
        assertThat(response.getContentAsString()).contains("corr-2");
    }
}