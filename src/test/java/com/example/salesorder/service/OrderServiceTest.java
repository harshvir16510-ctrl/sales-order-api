package com.example.salesorder.service;

import com.example.salesorder.domain.CatalogItem;
import com.example.salesorder.domain.Customer;
import com.example.salesorder.domain.SalesOrder;
import com.example.salesorder.dto.CreateOrderRequest;
import com.example.salesorder.dto.OrderItemRequest;
import com.example.salesorder.dto.OrderResponse;
import com.example.salesorder.exception.NotFoundException;
import com.example.salesorder.repository.CatalogItemRepository;
import com.example.salesorder.repository.CustomerRepository;
import com.example.salesorder.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private CatalogItem catalogItem;

    @BeforeEach
    void setUp() {
        // Set VAT rate using reflection since it's @Value injected
        try {
            java.lang.reflect.Field vatRateField = OrderService.class.getDeclaredField("vatRate");
            vatRateField.setAccessible(true);
            vatRateField.set(orderService, BigDecimal.valueOf(0.15));
        } catch (Exception e) {
            // Ignore if reflection fails
        }
        
        customer = new Customer("Test Customer");
        customer.setId(1L);

        catalogItem = new CatalogItem("SKU-001", "Test Item", BigDecimal.valueOf(10.00));
        catalogItem.setId(1L);
    }

    @Test
    void createOrder_Success() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        SalesOrder savedOrder = new SalesOrder();
        savedOrder.setId(1L);
        savedOrder.setCustomerId(1L);
        savedOrder.setSubtotal(BigDecimal.valueOf(20.00));
        savedOrder.setVat(BigDecimal.valueOf(3.00));
        savedOrder.setTotal(BigDecimal.valueOf(23.00));
        savedOrder.setStatus("CREATED");

        when(customerRepository.existsById(1L)).thenReturn(true);
        when(catalogItemRepository.findAllById(any())).thenReturn(List.of(catalogItem));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.customerId());
        assertEquals("Test Customer", response.customerName());
        assertEquals(1, response.items().size());
        assertNotNull(response.subtotal());
        assertNotNull(response.vat());
        assertNotNull(response.total());

        verify(salesOrderRepository, times(1)).save(any(SalesOrder.class));
    }

    @Test
    void createOrder_CustomerNotFound_ThrowsException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(customerRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> orderService.createOrder(request));
        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void getOrderById_Success() {
        // Given
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setStatus("CREATED");

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // When
        OrderResponse response = orderService.getOrderById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void cancelOrder_Success() {
        // Given
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setStatus("CREATED");

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);

        // When
        OrderResponse response = orderService.cancelOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals("CANCELLED", response.status());
        verify(salesOrderRepository, times(1)).save(order);
    }
}

