package com.example.salesorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long catalogItemId,
        @NotNull @Min(1) Integer quantity
) {}
