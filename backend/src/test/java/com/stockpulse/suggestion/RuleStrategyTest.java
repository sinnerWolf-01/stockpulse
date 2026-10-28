package com.stockpulse.suggestion;

import com.stockpulse.product.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RuleStrategyTest {

    private final RulePricingStrategy pricing = new RulePricingStrategy();
    private final RuleReorderStrategy reorder = new RuleReorderStrategy(7);

    private SignalContext context(BigDecimal price, int stock, int threshold, int velocity, BigDecimal peers) {
        return new SignalContext("PRD-001", Category.HOME, price, stock, threshold, velocity, peers, TriggerReason.MANUAL);
    }

    @Test
    void increasesPriceTenPercentWhenInventoryIsLow() {
        PricingRecommendation result = pricing.recommend(context(new BigDecimal("10.005"), 4, 5, 1, BigDecimal.ZERO));
        assertThat(result.recommendedPrice()).isEqualByComparingTo("11.01");
        assertThat(result.direction()).isEqualTo(Direction.INCREASE);
        assertThat(result.confidence()).isEqualByComparingTo("0.6");
    }

    @Test
    void increasesPriceFivePercentWhenDemandExceedsTwicePositivePeerAverage() {
        PricingRecommendation result = pricing.recommend(context(new BigDecimal("10.00"), 5, 5, 11, new BigDecimal("5")));
        assertThat(result.recommendedPrice()).isEqualByComparingTo("10.50");
        assertThat(result.direction()).isEqualTo(Direction.INCREASE);
    }

    @Test
    void holdsWhenDemandDoesNotExceedPeerRuleOrAverageIsZero() {
        PricingRecommendation ordinary = pricing.recommend(context(new BigDecimal("10.00"), 5, 5, 10, new BigDecimal("5")));
        PricingRecommendation zeroAverage = pricing.recommend(context(new BigDecimal("10.00"), 5, 5, 100, BigDecimal.ZERO));
        assertThat(ordinary.direction()).isEqualTo(Direction.HOLD);
        assertThat(ordinary.recommendedPrice()).isEqualByComparingTo("10.00");
        assertThat(zeroAverage.direction()).isEqualTo(Direction.HOLD);
    }

    @Test
    void calculatesMinimumReorderQuantityAndDefaultLeadTime() {
        ReorderRecommendation minimum = reorder.recommend(context(new BigDecimal("10.00"), 20, 5, 1, BigDecimal.ZERO));
        ReorderRecommendation target = reorder.recommend(context(new BigDecimal("10.00"), 4, 5, 1, BigDecimal.ZERO));
        assertThat(minimum.recommendedQuantity()).isEqualTo(1);
        assertThat(target.recommendedQuantity()).isEqualTo(11);
        assertThat(target.leadTimeDays()).isEqualTo(7);
        assertThat(target.confidence()).isEqualByComparingTo("0.6");
    }
}