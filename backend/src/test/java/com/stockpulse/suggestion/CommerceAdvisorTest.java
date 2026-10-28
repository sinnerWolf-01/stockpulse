package com.stockpulse.suggestion;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class CommerceAdvisorTest {

    @Mock ProductRepository productRepository;
    @Mock PricingSuggestionRepository pricingRepository;
    @Mock ReorderSuggestionRepository reorderRepository;

    @Test
    void calculatesPeerAverageExcludingCurrentProductAndReturnsPersistedPricingSuggestion() {
        Product current = new Product("PRD-001", "SKU-1", "Current", Category.HOME, new BigDecimal("10.00"), 5, 5, 11, com.stockpulse.product.ProductStatus.ACTIVE);
        Product peer = new Product("PRD-002", "SKU-2", "Peer", Category.HOME, new BigDecimal("10.00"), 5, 5, 5, com.stockpulse.product.ProductStatus.ACTIVE);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(current));
        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(current, peer));
        when(pricingRepository.findByActiveKey("PRD-001|DEMAND_SPIKE")).thenReturn(Optional.empty());
        when(pricingRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(PricingSuggestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PricingSuggestion result = new CommerceAdvisor(productRepository, pricingRepository, reorderRepository,
                new StrategyRegistry(List.of(new RulePricingStrategy()), List.of(new RuleReorderStrategy(7))),
                new SuggestionPersistenceWriter(pricingRepository, reorderRepository))
                .suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);

        assertThat(result.getRecommendedPrice()).isEqualByComparingTo("10.50");
        assertThat(result.getActiveKey()).isEqualTo("PRD-001|DEMAND_SPIKE");
    }

    @Test
    void persistsRuleFallbackWhenAiPricingFails() {
        Product current = new Product("PRD-001", "SKU-1", "Current", Category.HOME,
                new BigDecimal("10.00"), 5, 5, 11, com.stockpulse.product.ProductStatus.ACTIVE);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(current));
        when(productRepository.findByCategory(Category.HOME)).thenReturn(List.of(current));
        when(pricingRepository.findByActiveKey("PRD-001|DEMAND_SPIKE")).thenReturn(Optional.empty());
        when(pricingRepository.saveAndFlush(any(PricingSuggestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PricingStrategy failing = new PricingStrategy() {
            public String key() { return "ai"; }
            public PricingRecommendation recommend(SignalContext ignored) { throw new AdvisorException("offline"); }
        };
        StrategyRegistry registry = new StrategyRegistry(List.of(new RulePricingStrategy(), failing), List.of(new RuleReorderStrategy(7)));
        registry.selectPricing("ai");

        PricingSuggestion result = new CommerceAdvisor(productRepository, pricingRepository, reorderRepository,
                registry, new SuggestionPersistenceWriter(pricingRepository, reorderRepository))
                .suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);

        assertThat(result.getSource()).isEqualTo(SuggestionSource.RULE_FALLBACK);
    }
}