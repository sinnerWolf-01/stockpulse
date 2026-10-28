package com.stockpulse.suggestion;

public interface PricingStrategy {
    String key();

    PricingRecommendation recommend(SignalContext context);
}