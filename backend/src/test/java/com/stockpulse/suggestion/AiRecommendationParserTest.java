package com.stockpulse.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.product.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRecommendationParserTest {
    private final AiRecommendationParser parser = new AiRecommendationParser(new ObjectMapper());

    @Test
    void parsesFencedPricingAndClampsConfidence() {
        PricingRecommendation result = parser.parsePricing(
                "```json {\"recommendedPrice\":11.25,\"direction\":\"INCREASE\",\"confidence\":1.4,\"reasoning\":\"Demand is elevated\"} ```",
                new BigDecimal("10.00"));

        assertThat(result.recommendedPrice()).isEqualByComparingTo("11.25");
        assertThat(result.direction()).isEqualTo(Direction.INCREASE);
        assertThat(result.confidence()).isEqualByComparingTo("1");
    }

    @Test
    void defaultsMissingLeadTimeAndTruncatesReasoning() {
        String reasoning = "x".repeat(2100);
        ReorderRecommendation result = parser.parseReorder(
                "{\"recommendedQuantity\":4,\"confidence\":0.8,\"reasoning\":\"" + reasoning + "\"}");

        assertThat(result.recommendedQuantity()).isEqualTo(4);
        assertThat(result.leadTimeDays()).isEqualTo(7);
        assertThat(result.reasoning()).hasSize(2000);
    }

    @Test
    void rejectsDirectionMismatchAndBlankReasoning() {
        assertThatThrownBy(() -> parser.parsePricing(
                "{\"recommendedPrice\":9,\"direction\":\"INCREASE\",\"confidence\":0.5,\"reasoning\":\"x\"}",
                new BigDecimal("10"))).isInstanceOf(AdvisorException.class);
        assertThatThrownBy(() -> parser.parseReorder(
                "{\"recommendedQuantity\":1,\"confidence\":0.5,\"reasoning\":\" \"}"))
                .isInstanceOf(AdvisorException.class);
    }
}