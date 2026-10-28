package com.stockpulse.suggestion;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RulePricingStrategy implements PricingStrategy {

    private static final BigDecimal CONFIDENCE = new BigDecimal("0.6");

    @Override
    public String key() {
        return "rule";
    }

    @Override
    public PricingRecommendation recommend(SignalContext context) {
        BigDecimal multiplier;
        String reasoning;
        if (context.stockLevel() < context.reorderThreshold()) {
            multiplier = new BigDecimal("1.10");
            reasoning = "Stock is below the reorder threshold; increase price by 10%";
        } else if (context.categoryPeerAverage().signum() > 0
                && BigDecimal.valueOf(context.demandVelocity())
                .compareTo(context.categoryPeerAverage().multiply(BigDecimal.valueOf(2))) > 0) {
            multiplier = new BigDecimal("1.05");
            reasoning = "Demand velocity exceeds twice the category peer average; increase price by 5%";
        } else {
            return new PricingRecommendation(context.currentPrice().setScale(2, RoundingMode.HALF_UP),
                    Direction.HOLD, CONFIDENCE, "No low-stock or demand-spike pricing rule applies");
        }
        return new PricingRecommendation(context.currentPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP),
                Direction.INCREASE, CONFIDENCE, reasoning);
    }
}