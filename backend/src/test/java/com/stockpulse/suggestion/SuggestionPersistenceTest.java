package com.stockpulse.suggestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SuggestionPersistenceTest {

    @Autowired
    private PricingSuggestionRepository pricingRepository;

    @Autowired
    private ReorderSuggestionRepository reorderRepository;

    @BeforeEach
    void cleanSuggestions() {
        pricingRepository.deleteAll();
        reorderRepository.deleteAll();
    }

    @Test
    void persistsPricingSuggestionAndFindsPendingActiveKey() {
        PricingSuggestion saved = pricingRepository.saveAndFlush(new PricingSuggestion(
                "PRD-001", new BigDecimal("20.00"), new BigDecimal("22.00"), Direction.INCREASE,
                TriggerReason.INVENTORY_LOW, SuggestionSource.RULE, new BigDecimal("0.6000"),
                "Stock is below threshold", "PRD-001|INVENTORY_LOW"));

        PricingSuggestion found = pricingRepository
                .findByProductIdAndTriggerReasonAndStatus("PRD-001", TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)
                .orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getActiveKey()).isEqualTo("PRD-001|INVENTORY_LOW");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void activeKeyUniquenessProtectsConcurrentPendingDeduplication() {
        pricingRepository.saveAndFlush(new PricingSuggestion(
                "PRD-001", new BigDecimal("20.00"), new BigDecimal("22.00"), Direction.INCREASE,
                TriggerReason.DEMAND_SPIKE, SuggestionSource.RULE, new BigDecimal("0.6000"),
                "Demand increased", "PRD-001|DEMAND_SPIKE"));

        assertThatThrownBy(() -> pricingRepository.saveAndFlush(new PricingSuggestion(
                "PRD-002", new BigDecimal("30.00"), new BigDecimal("33.00"), Direction.INCREASE,
                TriggerReason.DEMAND_SPIKE, SuggestionSource.AI, new BigDecimal("0.9000"),
                "Demand increased", "PRD-001|DEMAND_SPIKE")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void resolvingPricingSuggestionClearsActiveKeyAndRecordsResolution() {
        PricingSuggestion suggestion = pricingRepository.saveAndFlush(new PricingSuggestion(
                "PRD-003", new BigDecimal("20.00"), new BigDecimal("20.00"), Direction.HOLD,
                TriggerReason.MANUAL, SuggestionSource.RULE, new BigDecimal("0.6000"),
                "No price change recommended", "PRD-003|MANUAL"));

        suggestion.resolve(SuggestionStatus.ACCEPTED, "Approved by merchandiser");
        pricingRepository.saveAndFlush(suggestion);

        PricingSuggestion resolved = pricingRepository.findById(suggestion.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(resolved.getActiveKey()).isNull();
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolutionNote()).isEqualTo("Approved by merchandiser");
        assertThat(pricingRepository.findByActiveKey("PRD-003|MANUAL")).isEmpty();
    }

    @Test
    void persistsReorderSuggestionAndClearsKeyOnRejection() {
        ReorderSuggestion suggestion = reorderRepository.saveAndFlush(new ReorderSuggestion(
                "PRD-004", 30, 7, TriggerReason.INVENTORY_LOW, SuggestionSource.RULE,
                new BigDecimal("0.6000"), "Restore three threshold cycles", "PRD-004|INVENTORY_LOW"));

        suggestion.resolve(SuggestionStatus.REJECTED, "Supplier unavailable");
        reorderRepository.saveAndFlush(suggestion);

        ReorderSuggestion resolved = reorderRepository.findById(suggestion.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(SuggestionStatus.REJECTED);
        assertThat(resolved.getActiveKey()).isNull();
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(reorderRepository.findByProductIdAndTriggerReasonAndStatus(
                "PRD-004", TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)).isEmpty();
    }

    @Test
    void rejectsResolvingSuggestionThatIsNotPending() {
        PricingSuggestion suggestion = new PricingSuggestion(
                "PRD-005", new BigDecimal("20.00"), new BigDecimal("21.00"), Direction.INCREASE,
                TriggerReason.MANUAL, SuggestionSource.RULE, new BigDecimal("0.6000"),
                "Manual recommendation", "PRD-005|MANUAL");
        suggestion.resolve(SuggestionStatus.REJECTED, "Not needed");

        assertThatThrownBy(() -> suggestion.resolve(SuggestionStatus.ACCEPTED, "Second resolution"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPendingPricingSuggestionWithoutActiveKey() {
        assertThatThrownBy(() -> new PricingSuggestion("PRD-006", new BigDecimal("20.00"), new BigDecimal("21.00"),
                Direction.INCREASE, TriggerReason.MANUAL, SuggestionSource.RULE, new BigDecimal("0.6000"),
                "Manual recommendation", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activeKey");
    }

    @Test
    void rejectsPendingReorderSuggestionWithoutActiveKey() {
        assertThatThrownBy(() -> new ReorderSuggestion("PRD-007", 10, 7, TriggerReason.MANUAL,
                SuggestionSource.RULE, new BigDecimal("0.6000"), "Manual recommendation", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activeKey");
    }
}