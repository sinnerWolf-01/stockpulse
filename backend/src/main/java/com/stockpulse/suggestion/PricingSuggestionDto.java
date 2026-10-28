package com.stockpulse.suggestion;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingSuggestionDto(
        Long id,
        String productId,
        BigDecimal currentPrice,
        BigDecimal recommendedPrice,
        Direction direction,
        TriggerReason triggerReason,
        SuggestionSource source,
        SuggestionStatus status,
        BigDecimal confidence,
        String reasoning,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        String resolutionNote) {

    public static PricingSuggestionDto from(PricingSuggestion suggestion) {
        return new PricingSuggestionDto(suggestion.getId(), suggestion.getProductId(), suggestion.getCurrentPrice(),
                suggestion.getRecommendedPrice(), suggestion.getDirection(), suggestion.getTriggerReason(),
                suggestion.getSource(), suggestion.getStatus(), suggestion.getConfidence(), suggestion.getReasoning(),
                suggestion.getCreatedAt(), suggestion.getUpdatedAt(), suggestion.getResolvedAt(), suggestion.getResolutionNote());
    }
}