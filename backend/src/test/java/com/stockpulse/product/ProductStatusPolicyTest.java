package com.stockpulse.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStatusPolicyTest {

    private final ProductStatusPolicy policy = new ProductStatusPolicy();

    @Test
    void outOfStockTakesPriorityOverPendingPricingReview() {
        assertThat(policy.derive(0, true)).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void pendingPricingReviewIsUsedWhenStockIsAvailable() {
        assertThat(policy.derive(5, true)).isEqualTo(ProductStatus.PRICE_REVIEW_PENDING);
    }

    @Test
    void availableProductWithoutPendingPricingIsActive() {
        assertThat(policy.derive(5, false)).isEqualTo(ProductStatus.ACTIVE);
    }
}