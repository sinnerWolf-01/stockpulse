package com.stockpulse.suggestion;

import java.math.BigDecimal;
import java.time.Instant;

public record ReorderSuggestionDto(
        Long id,
        String productId,
        int recommendedQuantity,
        int leadTimeDays,
        TriggerReason triggerReason,
        SuggestionSource source,
        SuggestionStatus status,
        BigDecimal confidence,
        String reasoning,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        String resolutionNote) {

    public static ReorderSuggestionDto from(ReorderSuggestion suggestion) {
        return new ReorderSuggestionDto(suggestion.getId(), suggestion.getProductId(), suggestion.getRecommendedQuantity(),
                suggestion.getLeadTimeDays(), suggestion.getTriggerReason(), suggestion.getSource(), suggestion.getStatus(),
                suggestion.getConfidence(), suggestion.getReasoning(), suggestion.getCreatedAt(), suggestion.getUpdatedAt(),
                suggestion.getResolvedAt(), suggestion.getResolutionNote());
    }
}