package com.example.salesorder;

import com.example.salesorder.domain.CatalogItem;
import com.example.salesorder.domain.Customer;
import com.example.salesorder.repository.CatalogItemRepository;
import com.example.salesorder.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class SalesOrderApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesOrderApiApplication.class, args);
    }

    // Seed sample catalog items and customers for quick testing
    @Bean
    public CommandLineRunner dataLoader(CatalogItemRepository catalogRepo, CustomerRepository customerRepo) {
        return args -> {
            if (catalogRepo.count() == 0) {
                catalogRepo.save(new CatalogItem("SKU-001", "Blue Widget", BigDecimal.valueOf(19.99)));
                catalogRepo.save(new CatalogItem("SKU-002", "Red Widget", BigDecimal.valueOf(29.50)));
            }
            if (customerRepo.count() == 0) {
                customerRepo.save(new Customer("Alice"));
                customerRepo.save(new Customer("Bob"));
            }
        };
    }
}
