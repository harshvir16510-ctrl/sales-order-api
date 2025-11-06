package com.example.salesorder.dto;

import java.time.LocalDate;

public record OrderQueryParams(
        LocalDate creationDateFrom,
        LocalDate creationDateTo,
        LocalDate cancellationDateFrom,
        LocalDate cancellationDateTo,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
    public OrderQueryParams {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }
}

