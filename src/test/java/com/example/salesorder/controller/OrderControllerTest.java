package com.example.salesorder.controller;

import com.example.salesorder.dto.OrderResponse;
import com.example.salesorder.dto.PageResponse;
import com.example.salesorder.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void getOrderById_Success() throws Exception {
        // Given
        OrderResponse response = new OrderResponse(
                1L, "ref-123", 1L, "Test Customer",
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "01/01/2024", null, "CREATED"
        );

        when(orderService.getOrderById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerName").value("Test Customer"));
    }

    @Test
    void listOrders_Success() throws Exception {
        // Given
        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(), 0, 20, 0L, 0, true, true
        );

        when(orderService.listOrders(any())).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

