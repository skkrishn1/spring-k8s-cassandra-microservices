package com.datastax.examples.product;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
// Explicit imports disambiguate `any` (also exported by org.hamcrest.Matchers)
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Unit tests for ProductController with DTO boundary testing.
 * Tests all REST endpoints with DTO validation and error scenarios.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private UUID productId;
    private String productName;
    private Product testProduct;
    private ProductRequestDto validRequestDto;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productName = "iPhone";
        testProduct = new Product(productName, productId, "Apple smartphone", 
                BigDecimal.valueOf(999.99), Instant.now());
        validRequestDto = new ProductRequestDto(productName, productId, "Apple smartphone", BigDecimal.valueOf(999.99));
    }

    // ====== Tests for DTO Binding (US-002) ======

    @Test
    void shouldBindProductRequestDtoAndNotProduct() throws Exception {
        // Given
        doNothing().when(productService).add(any(Product.class));

        // When & Then - POST with RequestDto should work
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(productName)))
                .andExpect(jsonPath("$.id", is(productId.toString())));

        verify(productService, times(1)).add(any(Product.class));
    }

    @Test
    void shouldReturnProductResponseDtoNotEntity() throws Exception {
        // Given
        when(productService.find(productName, productId)).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(get("/api/products/search/{name}/{id}", productName, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(productId.toString())))
                .andExpect(jsonPath("$.name", is(productName)))
                .andExpect(jsonPath("$.description", is("Apple smartphone")))
                .andExpect(jsonPath("$.price", closeTo(999.99, 0.001)))
                .andExpect(jsonPath("$.lastUpdated", notNullValue()));

        verify(productService, times(1)).find(productName, productId);
    }

    // ====== Tests for Validation (US-005) ======

    @Test
    void shouldRejectRequestWithMissingName() throws Exception {
        // Given - Missing name field
        ProductRequestDto invalidDto = new ProductRequestDto(null, productId, "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", containsString("name")));
    }

    @Test
    void shouldRejectRequestWithNullId() throws Exception {
        // Given - Null ID
        ProductRequestDto invalidDto = new ProductRequestDto("iPhone", null, "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("id")));
    }

    @Test
    void shouldRejectRequestWithNullPrice() throws Exception {
        // Given - Null price
        ProductRequestDto invalidDto = new ProductRequestDto("iPhone", productId, "description", null);

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("price")));
    }

    @Test
    void shouldRejectRequestWithNegativePrice() throws Exception {
        // Given - Negative price
        ProductRequestDto invalidDto = new ProductRequestDto("iPhone", productId, "description", BigDecimal.valueOf(-100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("price")));
    }

    @Test
    void shouldRejectRequestWithEmptyName() throws Exception {
        // Given - Empty name
        ProductRequestDto invalidDto = new ProductRequestDto("", productId, "description", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void shouldRejectRequestWithEmptyDescription() throws Exception {
        // Given - Empty description
        ProductRequestDto invalidDto = new ProductRequestDto("iPhone", productId, "", BigDecimal.valueOf(100));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void shouldAcceptValidRequest() throws Exception {
        // Given
        doNothing().when(productService).add(any(Product.class));

        // When & Then
        mockMvc.perform(post("/api/products/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(productName)))
                .andExpect(jsonPath("$.id", is(productId.toString())));

        verify(productService, times(1)).add(any(Product.class));
    }

    // ====== Tests for GET endpoints returning DTO ======

    @Test
    void shouldReturnProductResponseDtoListForSearch() throws Exception {
        // Given
        Iterable<Product> products = Arrays.asList(testProduct);
        when(productService.find(productName)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/products/search/{name}", productName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(productName)))
                .andExpect(jsonPath("$[0].id", is(productId.toString())))
                .andExpect(jsonPath("$[0].price", closeTo(999.99, 0.001)));

        verify(productService, times(1)).find(productName);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsFound() throws Exception {
        // Given
        when(productService.find(anyString())).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/products/search/{name}", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(productService, times(1)).find("nonexistent");
    }

    @Test
    void shouldReturnNotFoundWhenProductNotFound() throws Exception {
        // Given
        when(productService.find(anyString(), any(UUID.class))).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/products/search/{name}/{id}", productName, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(productService, times(1)).find(productName, productId);
    }

    // ====== Tests for DELETE endpoints ======

    @Test
    void shouldDeleteProductByName() throws Exception {
        // Given
        doNothing().when(productService).remove(productName);

        // When & Then
        mockMvc.perform(delete("/api/products/delete/{name}", productName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(productName));

        verify(productService, times(1)).remove(productName);
    }

    @Test
    void shouldDeleteProductByNameAndId() throws Exception {
        // Given
        doNothing().when(productService).remove(productName, productId);

        // When & Then
        mockMvc.perform(delete("/api/products/delete/{name}/{id}", productName, productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(productName)))
                .andExpect(content().string(containsString(productId.toString())));

        verify(productService, times(1)).remove(productName, productId);
    }

}
