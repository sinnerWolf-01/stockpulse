package com.stockpulse.suggestion;

import com.stockpulse.product.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {
    @Test
    void includesOperationalMetricsAndTriggerSpecificInstructions() {
        SignalContext context = new SignalContext("PRD-001", Category.HOME, new BigDecimal("10.00"),
                4, 5, 10, new BigDecimal("5"), TriggerReason.DEMAND_SPIKE);

        String prompt = new PromptBuilder().build(context, false);

        assertThat(prompt).contains("Peer average velocity: 5", "Velocity/peer ratio: 2.0000",
                "Days of cover: 0.40", "Demand velocity is unusually high", "recommendedPrice");
    }
}