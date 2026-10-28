package com.stockpulse.suggestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AiPricingStrategy implements PricingStrategy {
    private final PromptBuilder promptBuilder;
    private final LLMGateway gateway;
    private final AiRecommendationParser parser;
    private final BigDecimal maxChangePercent;
    private final BigDecimal minimumPrice;

    public AiPricingStrategy(PromptBuilder promptBuilder, LLMGateway gateway,
                             AiRecommendationParser parser,
                             @Value("${stockpulse.ai.pricing.max-change-percent:0.20}") BigDecimal maxChangePercent,
                             @Value("${stockpulse.ai.pricing.minimum-price:0.01}") BigDecimal minimumPrice) {
        if (maxChangePercent.signum() < 0 || minimumPrice.signum() <= 0) {
            throw new IllegalArgumentException("Invalid AI pricing guardrails");
        }
        this.promptBuilder = promptBuilder;
        this.gateway = gateway;
        this.parser = parser;
        this.maxChangePercent = maxChangePercent;
        this.minimumPrice = minimumPrice;
    }

    @Override
    public String key() { return "ai"; }

    @Override
    public PricingRecommendation recommend(SignalContext context) {
        PricingRecommendation recommendation = parser.parsePricing(
                gateway.complete(promptBuilder.build(context, false)), context.currentPrice());
        BigDecimal minimum = context.currentPrice().multiply(BigDecimal.ONE.subtract(maxChangePercent))
                .max(minimumPrice);
        BigDecimal maximum = context.currentPrice().multiply(BigDecimal.ONE.add(maxChangePercent));
        if (recommendation.recommendedPrice().compareTo(minimum) < 0
                || recommendation.recommendedPrice().compareTo(maximum) > 0) {
            throw new AdvisorException("AI recommended price exceeds configured guardrails");
        }
        return recommendation;
    }
}