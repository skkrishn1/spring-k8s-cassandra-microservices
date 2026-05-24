package com.datastax.examples.product;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests Problem+JSON error responses with RFC 7807 format.
 */
@WebMvcTest(ProductController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductRequestDto validRequestDto;

    @BeforeEach
    void setUp() {
        MDC.clear();
        validRequestDto = new ProductRequestDto("iPhone", UUID.randomUUID(), "description", BigDecimal.valueOf(100));
    }

    // ====== Tests for validation error (400) ======

    @Test
    void shouldReturnBadRequestWithProblemResponseForMissingName() throws Exception {
        // Given - Request with missing name
        ProductRequestDto invalidDto = new ProductRequestDto(null, UUID.randomUUID(), "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", notNullValue()))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.type", is("about:blank")));
    }

    @Test
    void shouldReturnBadRequestForInvalidPrice() throws Exception {
        // Given
        ProductRequestDto invalidDto = new ProductRequestDto("iPhone", UUID.randomUUID(), "description", BigDecimal.valueOf(-50));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", containsString("price")));
    }

    @Test
    void shouldIncludeTraceIdInErrorResponse() throws Exception {
        // Given
        ProductRequestDto invalidDto = new ProductRequestDto(null, UUID.randomUUID(), "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "test-trace-123")
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void shouldGenerateUUIDTraceIdWhenNotProvided() throws Exception {
        // Given - No X-Trace-Id header
        ProductRequestDto invalidDto = new ProductRequestDto(null, UUID.randomUUID(), "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.traceId", matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
    }

    @Test
    void shouldReturnValidProblemResponseForValidationError() throws Exception {
        // Given
        ProductRequestDto invalidDto = new ProductRequestDto("", UUID.randomUUID(), "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", notNullValue()))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void shouldReturnProblemResponseFor404FromResourceNotFoundException() throws Exception {
        // Given - service returns null, controller throws ResourceNotFoundException
        UUID missingId = UUID.randomUUID();
        when(productService.find("nonexistent", missingId)).thenReturn(null);

        // When & Then - GlobalExceptionHandler maps the exception to Problem+JSON 404
        mockMvc.perform(get("/api/products/search/{name}/{id}", "nonexistent", missingId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", containsString("nonexistent")))
                .andExpect(jsonPath("$.detail", containsString(missingId.toString())))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void shouldEchoTraceIdInResponseHeader() throws Exception {
        // Given - request with a client-supplied trace ID
        UUID missingId = UUID.randomUUID();
        when(productService.find("nonexistent", missingId)).thenReturn(null);

        // When & Then - TraceIdFilter must echo X-Trace-Id back so clients can correlate
        mockMvc.perform(get("/api/products/search/{name}/{id}", "nonexistent", missingId)
                .header("X-Trace-Id", "client-trace-abc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Trace-Id", "client-trace-abc"))
                .andExpect(jsonPath("$.traceId", is("client-trace-abc")));
    }

    @Test
    void shouldIncludeFieldLevelErrorDetails() throws Exception {
        // Given - Missing multiple fields
        String invalidJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", notNullValue()));
    }

}
