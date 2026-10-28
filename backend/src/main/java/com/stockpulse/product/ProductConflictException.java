package com.stockpulse.product;

public class ProductConflictException extends RuntimeException {
    public ProductConflictException(String message) {
        super(message);
    }
}