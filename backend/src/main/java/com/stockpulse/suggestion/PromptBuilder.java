package com.stockpulse.suggestion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {
    public String build(SignalContext context, boolean reorder) {
        BigDecimal ratio = context.categoryPeerAverage().signum() == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(context.demandVelocity()).divide(context.categoryPeerAverage(), 4, RoundingMode.HALF_UP);
        String trigger = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "Inventory is below the reorder threshold.";
            case DEMAND_SPIKE -> "Demand velocity is unusually high versus category peers.";
            case MANUAL, INITIAL -> "Provide a neutral recommendation based on the current product state.";
        };
        String instruction = reorder
                ? "Return reorder JSON with recommendedQuantity, leadTimeDays, confidence, and reasoning."
                : "Return pricing JSON with recommendedPrice, direction, confidence, and reasoning.";
        return "Product id: " + context.productId() + "\n"
                + "Category: " + context.category() + "\n"
                + "Current price: " + context.currentPrice() + "\n"
                + "Stock: " + context.stockLevel() + "\n"
                + "Reorder threshold: " + context.reorderThreshold() + "\n"
                + "Demand velocity: " + context.demandVelocity() + "\n"
                + "Peer average velocity: " + context.categoryPeerAverage() + "\n"
                + "Velocity/peer ratio: " + ratio + "\n"
                + "Days of cover: " + daysOfCover(context) + "\n"
                + trigger + "\n"
                + instruction + " JSON only; do not include markdown fences.";
    }

    private BigDecimal daysOfCover(SignalContext context) {
        if (context.demandVelocity() == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(context.stockLevel())
                .divide(BigDecimal.valueOf(context.demandVelocity()), 2, RoundingMode.HALF_UP);
    }
}