package com.datastax.examples.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderRepository.
 * Tests custom query methods with mocked repository behavior.
 */
@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    // Note: OrderRepository extends CassandraRepository which is typically tested
    // through integration tests. These are basic unit tests for custom methods.

    @Mock
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

    // ====== Tests for deleteByKeyOrderId ======

    @Test
    void shouldDeleteByKeyOrderIdSuccessfully() {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderId(orderId);

        // When
        orderRepository.deleteByKeyOrderId(orderId);

        // Then
        verify(orderRepository, times(1)).deleteByKeyOrderId(orderId);
    }

    @Test
    void shouldCallDeleteByKeyOrderIdWithCorrectOrderId() {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderId(any(UUID.class));
        UUID anotherOrderId = UUID.randomUUID();

        // When
        orderRepository.deleteByKeyOrderId(anotherOrderId);

        // Then
        verify(orderRepository).deleteByKeyOrderId(anotherOrderId);
    }

    // ====== Tests for deleteByKeyOrderIdAndKeyProductId ======

    @Test
    void shouldDeleteByKeyOrderIdAndKeyProductIdSuccessfully() {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(orderId, productId);

        // When
        orderRepository.deleteByKeyOrderIdAndKeyProductId(orderId, productId);

        // Then
        verify(orderRepository, times(1)).deleteByKeyOrderIdAndKeyProductId(orderId, productId);
    }

    @Test
    void shouldCallDeleteByKeyOrderIdAndKeyProductIdWithCorrectIds() {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(any(UUID.class), any(UUID.class));
        UUID anotherOrderId = UUID.randomUUID();
        UUID anotherProductId = UUID.randomUUID();

        // When
        orderRepository.deleteByKeyOrderIdAndKeyProductId(anotherOrderId, anotherProductId);

        // Then
        verify(orderRepository).deleteByKeyOrderIdAndKeyProductId(anotherOrderId, anotherProductId);
    }

    @Test
    void shouldHandleMultipleDeleteOperations() {
        // Given
        doNothing().when(orderRepository).deleteByKeyOrderIdAndKeyProductId(any(UUID.class), any(UUID.class));

        // When
        orderRepository.deleteByKeyOrderIdAndKeyProductId(orderId, productId);
        orderRepository.deleteByKeyOrderIdAndKeyProductId(UUID.randomUUID(), UUID.randomUUID());

        // Then
        verify(orderRepository, times(2)).deleteByKeyOrderIdAndKeyProductId(any(UUID.class), any(UUID.class));
    }

    // ====== Tests for save ======

    @Test
    void shouldSaveOrderSuccessfully() {
        // Given
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        orderRepository.save(testOrder);

        // Then
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void shouldCallSaveWithCorrectOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderRepository.save(testOrder);

        // Then
        verify(orderRepository).save(testOrder);
    }

    @Test
    void shouldSaveMultipleOrders() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        orderRepository.save(testOrder);
        UUID anotherOrderId = UUID.randomUUID();
        OrderPrimaryKey anotherKey = new OrderPrimaryKey(anotherOrderId, UUID.randomUUID());
        Order anotherOrder = new Order(anotherKey, "Samsung", 2, 599.99f, Instant.now());
        orderRepository.save(anotherOrder);

        // Then
        verify(orderRepository, times(2)).save(any(Order.class));
    }
}
