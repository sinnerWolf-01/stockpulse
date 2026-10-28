package com.stockpulse.suggestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RuleReorderStrategy implements ReorderStrategy {

    private static final BigDecimal CONFIDENCE = new BigDecimal("0.6");
    private final int leadTimeDays;

    public RuleReorderStrategy(@Value("${stockpulse.reorder.lead-time-days:7}") int leadTimeDays) {
        if (leadTimeDays < 0) throw new IllegalArgumentException("leadTimeDays cannot be negative");
        this.leadTimeDays = leadTimeDays;
    }

    @Override
    public String key() {
        return "rule";
    }

    @Override
    public ReorderRecommendation recommend(SignalContext context) {
        int target = context.reorderThreshold() * 3;
        int quantity = Math.max(1, target - context.stockLevel());
        return new ReorderRecommendation(quantity, leadTimeDays, CONFIDENCE,
                "Restore stock to three reorder-threshold cycles");
    }
}