package com.stockpulse.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.product.Category;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class AiStrategyTest {
    private final SignalContext context = new SignalContext("PRD-001", Category.HOME,
            new BigDecimal("10.00"), 5, 5, 8, new BigDecimal("4"), TriggerReason.DEMAND_SPIKE);

    @Test
    void parsesAndReturnsAiPricingWithinGuardrails() {
        LLMGateway gateway = prompt -> "{\"recommendedPrice\":11,\"direction\":\"INCREASE\",\"confidence\":0.8,\"reasoning\":\"Demand is high\"}";
        AiPricingStrategy strategy = new AiPricingStrategy(new PromptBuilder(), gateway,
                new AiRecommendationParser(new ObjectMapper()), new BigDecimal("0.20"), new BigDecimal("0.01"));

        assertThat(strategy.recommend(context).recommendedPrice()).isEqualByComparingTo("11.00");
    }

    @Test
    void rejectsAiReorderAboveConfiguredMultiplier() {
        LLMGateway gateway = prompt -> "{\"recommendedQuantity\":51,\"leadTimeDays\":7,\"confidence\":0.8,\"reasoning\":\"Restore stock\"}";
        AiReorderStrategy strategy = new AiReorderStrategy(new PromptBuilder(), gateway,
                new AiRecommendationParser(new ObjectMapper()), 10);

        assertThatThrownBy(() -> strategy.recommend(context))
                .isInstanceOf(AdvisorException.class);
    }

    @Test
    void registryRejectsUnknownRuntimeStrategy() {
        StrategyRegistry registry = new StrategyRegistry(
                java.util.List.of(new RulePricingStrategy()), java.util.List.of(new RuleReorderStrategy(7)));

        assertThatThrownBy(() -> registry.selectPricing("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void combinedSelectionValidatesBeforeChangingEitherStrategy() {
        PricingStrategy alternatePricing = new RulePricingStrategy() {
            @Override
            public String key() {
                return "alternate-pricing";
            }
        };
        ReorderStrategy alternateReorder = new RuleReorderStrategy(7) {
            @Override
            public String key() {
                return "alternate-reorder";
            }
        };
        StrategyRegistry registry = new StrategyRegistry(
                java.util.List.of(new RulePricingStrategy(), alternatePricing),
                java.util.List.of(new RuleReorderStrategy(7), alternateReorder));
        registry.select("alternate-pricing", "alternate-reorder");

        assertThatThrownBy(() -> registry.select("rule", "unknown-reorder"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.selectedPricingKey()).isEqualTo("alternate-pricing");
        assertThat(registry.selectedReorderKey()).isEqualTo("alternate-reorder");
    }
}