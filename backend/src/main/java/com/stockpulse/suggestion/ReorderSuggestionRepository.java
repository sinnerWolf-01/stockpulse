package com.stockpulse.suggestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {
    Optional<ReorderSuggestion> findByActiveKey(String activeKey);
    Optional<ReorderSuggestion> findByProductIdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);
    List<ReorderSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}