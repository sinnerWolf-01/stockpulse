package com.stockpulse.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_id_counters")
public class ProductIdCounter {

    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String name;

    @Column(nullable = false)
    private long nextValue;

    protected ProductIdCounter() {
    }

    public ProductIdCounter(String name, long nextValue) {
        this.name = name;
        this.nextValue = nextValue;
    }

    public long allocate() {
        return nextValue++;
    }

    public long getNextValue() {
        return nextValue;
    }
}