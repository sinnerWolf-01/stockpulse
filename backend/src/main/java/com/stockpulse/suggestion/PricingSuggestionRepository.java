package com.stockpulse.suggestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
    Optional<PricingSuggestion> findByActiveKey(String activeKey);
    Optional<PricingSuggestion> findByProductIdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);
    List<PricingSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}