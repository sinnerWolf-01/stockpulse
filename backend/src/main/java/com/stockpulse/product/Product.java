package com.stockpulse.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Min(0)
    @Column(nullable = false)
    private int stockLevel;

    @Min(0)
    @Column(nullable = false)
    private int reorderThreshold;

    @Min(0)
    @Column(nullable = false)
    private int demandVelocity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    // These nullable fields are deliberate seams for the later margin/supplier work.
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal marginFloor;

    @Column(length = 64)
    private String supplierId;

    @Version
    @Column(nullable = false)
    // Assigned String ids require a nullable wrapper so Spring Data can identify new entities correctly.
    private Long version;

    protected Product() {
        // Required by JPA.
    }

    public Product(String id, String sku, String name, Category category,
                   BigDecimal currentPrice, int stockLevel, int reorderThreshold,
                   int demandVelocity, ProductStatus status) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity;
        this.status = status;
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public int getStockLevel() { return stockLevel; }
    public int getReorderThreshold() { return reorderThreshold; }
    public int getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getMarginFloor() { return marginFloor; }
    public String getSupplierId() { return supplierId; }
    public Long getVersion() { return version; }

    void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    // Package-private keeps status changes inside the product domain package.
    void setStatus(ProductStatus status) { this.status = status; }

    void setStockLevel(int stockLevel) { this.stockLevel = stockLevel; }
}