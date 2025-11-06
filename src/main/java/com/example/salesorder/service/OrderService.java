package com.example.salesorder.service;

import com.example.salesorder.dto.*;
import com.example.salesorder.domain.CatalogItem;
import com.example.salesorder.domain.Customer;
import com.example.salesorder.domain.OrderItem;
import com.example.salesorder.domain.SalesOrder;
import com.example.salesorder.exception.NotFoundException;
import com.example.salesorder.repository.CatalogItemRepository;
import com.example.salesorder.repository.CustomerRepository;
import com.example.salesorder.repository.SalesOrderRepository;
import com.example.salesorder.util.DateFormatter;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CatalogItemRepository catalogItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;

    @Value("${app.vat-rate:0.15}")
    private BigDecimal vatRate;

    public OrderService(CatalogItemRepository catalogItemRepository,
                        SalesOrderRepository salesOrderRepository,
                        CustomerRepository customerRepository) {
        this.catalogItemRepository = catalogItemRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        // Validate customer
        var customerId = req.customerId();
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Customer not found: " + customerId);
        }

        // Load catalog items
        List<Long> ids = req.items().stream().map(OrderItemRequest::catalogItemId).distinct().toList();
        List<CatalogItem> catalogItems = catalogItemRepository.findAllById(ids);
        Map<Long, CatalogItem> catalogMap = catalogItems.stream().collect(Collectors.toMap(CatalogItem::getId, c -> c));

        SalesOrder order = new SalesOrder();
        order.setCustomerId(customerId);

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.items()) {
            CatalogItem cat = catalogMap.get(itemReq.catalogItemId());
            if (cat == null) throw new NotFoundException("Catalog item not found: " + itemReq.catalogItemId());
            OrderItem oi = new OrderItem();
            oi.setCatalogItemId(cat.getId());
            oi.setItemName(cat.getName());
            oi.setItemPrice(cat.getPrice());
            oi.setQuantity(itemReq.quantity());
            BigDecimal totalPrice = cat.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            oi.setTotalPrice(totalPrice);
            order.addItem(oi);
            subtotal = subtotal.add(totalPrice);
        }

        BigDecimal vat = subtotal.multiply(vatRate);
        BigDecimal total = subtotal.add(vat);

        order.setSubtotal(subtotal);
        order.setVat(vat);
        order.setTotal(total);
        order.setStatus("CREATED");

        var saved = salesOrderRepository.save(order);

        Customer customer = customerRepository.findById(saved.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found: " + saved.getCustomerId()));

        var itemsResp = saved.getItems().stream()
                .map(it -> new OrderResponse.OrderItemResponse(
                        it.getId(),
                        it.getItemName(),
                        it.getItemPrice(),
                        it.getQuantity(),
                        it.getTotalPrice()
                ))
                .toList();

        return new OrderResponse(
                saved.getId(),
                saved.getOrderReference(),
                saved.getCustomerId(),
                customer.getName(),
                itemsResp,
                saved.getSubtotal(),
                saved.getVat(),
                saved.getTotal(),
                DateFormatter.formatInstant(saved.getCreatedAt()),
                DateFormatter.formatInstant(saved.getCancelledAt()),
                saved.getStatus()
        );
    }

    public PageResponse<OrderResponse> listOrders(OrderQueryParams params) {
        // Convert LocalDate to Instant for query
        Instant creationDateFrom = params.creationDateFrom() != null 
                ? params.creationDateFrom().atStartOfDay(ZoneId.systemDefault()).toInstant() 
                : null;
        Instant creationDateTo = params.creationDateTo() != null 
                ? params.creationDateTo().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() 
                : null;
        Instant cancellationDateFrom = params.cancellationDateFrom() != null 
                ? params.cancellationDateFrom().atStartOfDay(ZoneId.systemDefault()).toInstant() 
                : null;
        Instant cancellationDateTo = params.cancellationDateTo() != null 
                ? params.cancellationDateTo().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() 
                : null;

        // Create sort
        Sort sort = Sort.by("desc".equalsIgnoreCase(params.sortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC, 
                params.sortBy());
        Pageable pageable = PageRequest.of(params.page(), params.size(), sort);

        // Query with filters
        Page<SalesOrder> page = salesOrderRepository.findByFilters(
                creationDateFrom, creationDateTo, 
                cancellationDateFrom, cancellationDateTo, 
                pageable
        );

        // Map to response
        List<OrderResponse> content = page.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public OrderResponse getOrderById(Long id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(SalesOrder saved) {
        Customer customer = customerRepository.findById(saved.getCustomerId())
                .orElse(null); // Handle case where customer might be deleted

        var itemsResp = saved.getItems().stream()
                .map(it -> new OrderResponse.OrderItemResponse(
                        it.getId(),
                        it.getItemName(),
                        it.getItemPrice(),
                        it.getQuantity(),
                        it.getTotalPrice()
                )).toList();

        return new OrderResponse(
                saved.getId(),
                saved.getOrderReference(),
                saved.getCustomerId(),
                customer != null ? customer.getName() : "Unknown",
                itemsResp,
                saved.getSubtotal(),
                saved.getVat(),
                saved.getTotal(),
                DateFormatter.formatInstant(saved.getCreatedAt()),
                DateFormatter.formatInstant(saved.getCancelledAt()),
                saved.getStatus()
        );
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        var order = salesOrderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
        if ("CANCELLED".equals(order.getStatus())) {
            // already cancelled; return current state
        } else {
            order.setStatus("CANCELLED");
            order.setCancelledAt(java.time.Instant.now());
            salesOrderRepository.save(order);
        }

        return mapToOrderResponse(order);
    }
}
