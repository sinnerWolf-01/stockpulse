package com.stockpulse.suggestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AiRecommendationParser {
    private final ObjectMapper objectMapper;

    public AiRecommendationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PricingRecommendation parsePricing(String content, BigDecimal currentPrice) {
        JsonNode node = node(content);
        BigDecimal price = decimal(node, "recommendedPrice");
        Direction direction = enumValue(node, "direction", Direction.class);
        BigDecimal confidence = confidence(node);
        String reasoning = reasoning(node);
        if (price.signum() <= 0) throw new AdvisorException("AI recommended price must be positive");
        if (direction == Direction.INCREASE && price.compareTo(currentPrice) <= 0
                || direction == Direction.DECREASE && price.compareTo(currentPrice) >= 0
                || direction == Direction.HOLD && price.compareTo(currentPrice) != 0) {
            throw new AdvisorException("AI pricing direction does not match recommended price");
        }
        return new PricingRecommendation(price.setScale(2, RoundingMode.HALF_UP), direction, confidence, reasoning);
    }

    public ReorderRecommendation parseReorder(String content) {
        JsonNode node = node(content);
        int quantity = integer(node, "recommendedQuantity");
        int leadTime = node.has("leadTimeDays") ? integer(node, "leadTimeDays") : 7;
        if (quantity <= 0 || leadTime < 0) throw new AdvisorException("AI reorder values are invalid");
        return new ReorderRecommendation(quantity, leadTime, confidence(node), reasoning(node));
    }

    private JsonNode node(String content) {
        if (content == null || content.isBlank()) throw new AdvisorException("AI response is blank");
        String json = content.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try { return objectMapper.readTree(json); }
        catch (Exception exception) { throw new AdvisorException("AI response is not valid JSON", exception); }
    }

    private BigDecimal decimal(JsonNode node, String name) {
        if (!node.has(name) || !node.get(name).isNumber()) throw new AdvisorException("AI field is missing: " + name);
        return node.get(name).decimalValue();
    }

    private int integer(JsonNode node, String name) {
        if (!node.has(name) || !node.get(name).canConvertToInt()) throw new AdvisorException("AI field is invalid: " + name);
        return node.get(name).intValue();
    }

    private BigDecimal confidence(JsonNode node) {
        BigDecimal value = decimal(node, "confidence");
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String reasoning(JsonNode node) {
        String value = node.has("reasoning") ? node.get("reasoning").asText() : "";
        if (value.isBlank()) throw new AdvisorException("AI reasoning is required");
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }

    private <E extends Enum<E>> E enumValue(JsonNode node, String name, Class<E> type) {
        if (!node.has(name)) throw new AdvisorException("AI field is missing: " + name);
        try { return Enum.valueOf(type, node.get(name).asText().toUpperCase()); }
        catch (Exception exception) { throw new AdvisorException("AI field is invalid: " + name, exception); }
    }
}