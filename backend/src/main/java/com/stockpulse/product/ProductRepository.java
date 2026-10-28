package com.stockpulse.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    boolean existsBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory(Category category);

    List<Product> findByStatusAndCategory(ProductStatus status, Category category);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Product p
               set p.stockLevel = p.stockLevel - :quantity,
                   p.demandVelocity = p.demandVelocity + :quantity,
                   p.version = p.version + 1
             where p.id = :id
               and p.stockLevel >= :quantity
            """)
    int decrementStockAndIncrementVelocity(@Param("id") String id, @Param("quantity") int quantity);
}