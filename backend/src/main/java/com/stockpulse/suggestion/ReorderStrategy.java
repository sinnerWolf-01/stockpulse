package com.stockpulse.suggestion;

public interface ReorderStrategy {
    String key();

    ReorderRecommendation recommend(SignalContext context);
}