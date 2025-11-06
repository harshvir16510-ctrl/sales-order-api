package com.example.salesorder.integration;

import com.example.salesorder.domain.CatalogItem;
import com.example.salesorder.domain.Customer;
import com.example.salesorder.dto.CreateOrderRequest;
import com.example.salesorder.dto.OrderItemRequest;
import com.example.salesorder.dto.OrderResponse;
import com.example.salesorder.repository.CatalogItemRepository;
import com.example.salesorder.repository.CustomerRepository;
import com.example.salesorder.repository.SalesOrderRepository;
import com.example.salesorder.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    private Customer customer;
    private CatalogItem catalogItem;

    @BeforeEach
    void setUp() {
        salesOrderRepository.deleteAll();
        catalogItemRepository.deleteAll();
        customerRepository.deleteAll();

        customer = customerRepository.save(new Customer("Integration Test Customer"));
        catalogItem = catalogItemRepository.save(
                new CatalogItem("SKU-INT-001", "Integration Test Item", BigDecimal.valueOf(25.50))
        );
    }

    @Test
    void createOrder_IntegrationTest() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(),
                List.of(new OrderItemRequest(catalogItem.getId(), 3))
        );

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertNotNull(response);
        assertEquals(customer.getId(), response.customerId());
        assertEquals(customer.getName(), response.customerName());
        assertEquals(1, response.items().size());
        assertTrue(response.subtotal().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(response.creationDate());
    }

    @Test
    void getOrderById_IntegrationTest() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(),
                List.of(new OrderItemRequest(catalogItem.getId(), 2))
        );
        OrderResponse created = orderService.createOrder(request);

        // When
        OrderResponse retrieved = orderService.getOrderById(created.id());

        // Then
        assertNotNull(retrieved);
        assertEquals(created.id(), retrieved.id());
        assertEquals(customer.getName(), retrieved.customerName());
    }

    @Test
    void cancelOrder_IntegrationTest() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                customer.getId(),
                List.of(new OrderItemRequest(catalogItem.getId(), 1))
        );
        OrderResponse created = orderService.createOrder(request);

        // When
        OrderResponse cancelled = orderService.cancelOrder(created.id());

        // Then
        assertEquals("CANCELLED", cancelled.status());
        assertNotNull(cancelled.cancellationDate());
    }
}

