package com.example.salesorder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "catalog_item")
@Getter
@Setter
@NoArgsConstructor
public class CatalogItem {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sku;
    private String name;
    private BigDecimal price;
    private Instant updatedAt = Instant.now();

    // Constructor for creating new catalog items (without ID)
    public CatalogItem(String sku, String name, BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
    }
}
