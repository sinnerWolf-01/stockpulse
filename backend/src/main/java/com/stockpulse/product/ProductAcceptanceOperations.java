package com.stockpulse.product;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductAcceptanceOperations {

    private final ProductStatusPolicy statusPolicy;

    public ProductAcceptanceOperations(ProductStatusPolicy statusPolicy) {
        this.statusPolicy = statusPolicy;
    }

    public void applyAcceptedPrice(Product product, BigDecimal price, boolean hasPendingPricingSuggestion) {
        product.setCurrentPrice(price);
        product.setStatus(statusPolicy.derive(product.getStockLevel(), hasPendingPricingSuggestion));
    }

    public void applyAcceptedReorder(Product product, int quantity, boolean hasPendingPricingSuggestion) {
        product.setStockLevel(product.getStockLevel() + quantity);
        product.setStatus(statusPolicy.derive(product.getStockLevel(), hasPendingPricingSuggestion));
    }

    public void deriveStatus(Product product, boolean hasPendingPricingSuggestion) {
        product.setStatus(statusPolicy.derive(product.getStockLevel(), hasPendingPricingSuggestion));
    }

    public void markPricingReviewPending(Product product) {
        product.setStatus(statusPolicy.derive(product.getStockLevel(), true));
    }
}