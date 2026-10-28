package com.stockpulse.product;

import org.springframework.stereotype.Component;

@Component
public class ProductStatusPolicy {

    public ProductStatus derive(int stockLevel, boolean hasPendingPricingSuggestion) {
        if (stockLevel == 0) {
            return ProductStatus.OUT_OF_STOCK;
        }
        if (hasPendingPricingSuggestion) {
            return ProductStatus.PRICE_REVIEW_PENDING;
        }
        return ProductStatus.ACTIVE;
    }
}