package com.example.salesorder.repository;

import com.example.salesorder.domain.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    
    @Query("SELECT o FROM SalesOrder o WHERE " +
           "(:creationDateFrom IS NULL OR o.createdAt >= :creationDateFrom) AND " +
           "(:creationDateTo IS NULL OR o.createdAt <= :creationDateTo) AND " +
           "(:cancellationDateFrom IS NULL OR o.cancelledAt IS NULL OR o.cancelledAt >= :cancellationDateFrom) AND " +
           "(:cancellationDateTo IS NULL OR o.cancelledAt IS NULL OR o.cancelledAt <= :cancellationDateTo)")
    Page<SalesOrder> findByFilters(
            @Param("creationDateFrom") Instant creationDateFrom,
            @Param("creationDateTo") Instant creationDateTo,
            @Param("cancellationDateFrom") Instant cancellationDateFrom,
            @Param("cancellationDateTo") Instant cancellationDateTo,
            Pageable pageable
    );
    
    Optional<SalesOrder> findById(Long id);
}
