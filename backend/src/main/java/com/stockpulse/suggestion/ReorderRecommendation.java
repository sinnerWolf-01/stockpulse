package com.stockpulse.suggestion;

import java.math.BigDecimal;

public record ReorderRecommendation(
        int recommendedQuantity,
        int leadTimeDays,
        BigDecimal confidence,
        String reasoning) {
}