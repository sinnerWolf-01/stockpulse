package com.stockpulse.suggestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiReorderStrategy implements ReorderStrategy {
    private final PromptBuilder promptBuilder;
    private final LLMGateway gateway;
    private final AiRecommendationParser parser;
    private final int maxMultiplier;

    public AiReorderStrategy(PromptBuilder promptBuilder, LLMGateway gateway,
                             AiRecommendationParser parser,
                             @Value("${stockpulse.ai.reorder.max-multiplier:10}") int maxMultiplier) {
        if (maxMultiplier < 1) throw new IllegalArgumentException("max reorder multiplier must be positive");
        this.promptBuilder = promptBuilder;
        this.gateway = gateway;
        this.parser = parser;
        this.maxMultiplier = maxMultiplier;
    }

    @Override
    public String key() { return "ai"; }

    @Override
    public ReorderRecommendation recommend(SignalContext context) {
        ReorderRecommendation recommendation = parser.parseReorder(
                gateway.complete(promptBuilder.build(context, true)));
        long maximum = Math.max(1L, (long) context.reorderThreshold() * maxMultiplier);
        if (recommendation.recommendedQuantity() > maximum) {
            throw new AdvisorException("AI recommended reorder quantity exceeds configured limit");
        }
        return recommendation;
    }
}