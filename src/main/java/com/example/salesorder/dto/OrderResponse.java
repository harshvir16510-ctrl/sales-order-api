package com.example.salesorder.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderReference,
        Long customerId,
        String customerName,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        String creationDate,
        String cancellationDate,
        String status
) {
    public record OrderItemResponse(
            Long id,
            String itemName,
            BigDecimal itemPrice,
            Integer quantity,
            BigDecimal totalPrice
    ) {}
}
