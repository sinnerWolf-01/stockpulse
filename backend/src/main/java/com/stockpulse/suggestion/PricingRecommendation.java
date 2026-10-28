package com.stockpulse.suggestion;

import java.math.BigDecimal;

public record PricingRecommendation(
        BigDecimal recommendedPrice,
        Direction direction,
        BigDecimal confidence,
        String reasoning) {
}