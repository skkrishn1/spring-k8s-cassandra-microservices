package com.datastax.examples.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController.
 * Tests REST endpoints for order management using MockMvc.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;

    private UUID orderId;
    private UUID productId;
    private Order testOrder;
    private OrderPrimaryKey orderKey;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize test data
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderKey = new OrderPrimaryKey(orderId, productId);
        testOrder = new Order(orderKey, "iPhone", 1, 999.99f, Instant.now());
    }

    // ====== Tests for POST /api/orders/add ======

    @Test
    void shouldAddOrderSuccessfully() throws Exception {
        // Given
        doNothing().when(orderRepository).save(any(Order.class));

        String orderJson = String.format(
                "{\"key\":{\"orderId\":\"%s\",\"productId\":\"%s\"},\"productName\":\"iPhone\",\"productQuantity\":1,\"productPrice\":999.99}",
                orderId, productId
        );

        // When & Then
        mockMvc.perform(post("/orders/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void shouldReturnOrderWhenAdded() throws Exception {
        // Given
        doNothing().when(orderRepository).save(any(Order.class));

        String orderJson = String.format(
                "{\"key\":{\"orderId\":\"%s\",\"productId\":\"%s\"},\"productName\":\"iPhone\",\"productQuantity\":1,\"productPrice\":999.99}",
                orderId, productId
        );

        // When & Then
        mockMvc.perform(post("/orders/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("iPhone"))
                .andExpect(jsonPath("$.productQuantity").value(1));

        verify(orderRepository).save(any(Order.class));
    }

    // ====== Tests for DELETE /api/orders/delete/order ======

    @Test
    void shouldDeleteOrderSuccessfully() throws Exception {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderId(orderId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/order")
                .param("orderId", orderId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(orderId.toString()));

        verify(orderRepository, times(1)).deleteByKeyOrderId(orderId);
    }

    @Test
    void shouldReturnOrderIdWhenOrderDeleted() throws Exception {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderId(orderId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/order")
                .param("orderId", orderId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(orderId.toString()));

        verify(orderRepository).deleteByKeyOrderId(orderId);
    }

    @Test
    void shouldHandleDeleteOrderWithDifferentOrderIds() throws Exception {
        // Given
        UUID anotherOrderId = UUID.randomUUID();
        doNothing().when(orderRepository).deleteByKeyOrderId(anotherOrderId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/order")
                .param("orderId", anotherOrderId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(anotherOrderId.toString()));

        verify(orderRepository).deleteByKeyOrderId(anotherOrderId);
    }

    // ====== Tests for DELETE /api/orders/delete/product-from-order ======

    @Test
    void shouldDeleteProductFromOrderSuccessfully() throws Exception {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(orderId, productId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/product-from-order")
                .param("orderId", orderId.toString())
                .param("productId", productId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(orderId + "," + productId));

        verify(orderRepository, times(1)).deleteByKeyOrderIdAndKeyProductId(orderId, productId);
    }

    @Test
    void shouldReturnOrderAndProductIdWhenProductDeleted() throws Exception {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(orderId, productId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/product-from-order")
                .param("orderId", orderId.toString())
                .param("productId", productId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(orderId + "," + productId));

        verify(orderRepository).deleteByKeyOrderIdAndKeyProductId(orderId, productId);
    }

    @Test
    void shouldHandleDeleteProductWithDifferentIds() throws Exception {
        // Given
        UUID anotherOrderId = UUID.randomUUID();
        UUID anotherProductId = UUID.randomUUID();
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(anotherOrderId, anotherProductId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/product-from-order")
                .param("orderId", anotherOrderId.toString())
                .param("productId", anotherProductId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(anotherOrderId + "," + anotherProductId));

        verify(orderRepository).deleteByKeyOrderIdAndKeyProductId(anotherOrderId, anotherProductId);
    }

    @Test
    void shouldCallRepositoryDeleteMethodOnce() throws Exception {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(orderId, productId);

        // When & Then
        mockMvc.perform(delete("/orders/delete/product-from-order")
                .param("orderId", orderId.toString())
                .param("productId", productId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderRepository, times(1)).deleteByKeyOrderIdAndKeyProductId(orderId, productId);
    }
}
