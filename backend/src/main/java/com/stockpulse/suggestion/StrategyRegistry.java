package com.stockpulse.suggestion;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StrategyRegistry {

    private final Map<String, PricingStrategy> pricingStrategies;
    private final Map<String, ReorderStrategy> reorderStrategies;
    private volatile String selectedPricingKey = "rule";
    private volatile String selectedReorderKey = "rule";

    public StrategyRegistry(List<PricingStrategy> pricingStrategies, List<ReorderStrategy> reorderStrategies) {
        this.pricingStrategies = pricingStrategies.stream().collect(Collectors.toUnmodifiableMap(PricingStrategy::key, Function.identity()));
        this.reorderStrategies = reorderStrategies.stream().collect(Collectors.toUnmodifiableMap(ReorderStrategy::key, Function.identity()));
    }

    @Value("${stockpulse.strategy.pricing:rule}")
    void configurePricing(String key) { selectPricing(key); }

    @Value("${stockpulse.strategy.reorder:rule}")
    void configureReorder(String key) { selectReorder(key); }

    public PricingStrategy pricing(String key) {
        PricingStrategy strategy = pricingStrategies.get(key);
        if (strategy == null) throw new IllegalArgumentException("Unknown pricing strategy: " + key);
        return strategy;
    }

    public ReorderStrategy reorder(String key) {
        ReorderStrategy strategy = reorderStrategies.get(key);
        if (strategy == null) throw new IllegalArgumentException("Unknown reorder strategy: " + key);
        return strategy;
    }

    public PricingStrategy defaultPricing() { return pricing("rule"); }
    public ReorderStrategy defaultReorder() { return reorder("rule"); }

    public synchronized void selectPricing(String key) {
        pricing(key);
        selectedPricingKey = key;
    }

    public synchronized void selectReorder(String key) {
        reorder(key);
        selectedReorderKey = key;
    }

    public synchronized void select(String pricingKey, String reorderKey) {
        if (pricingKey != null) pricing(pricingKey);
        if (reorderKey != null) reorder(reorderKey);
        if (pricingKey != null) selectedPricingKey = pricingKey;
        if (reorderKey != null) selectedReorderKey = reorderKey;
    }

    public PricingStrategy selectedPricing() { return pricing(selectedPricingKey); }
    public ReorderStrategy selectedReorder() { return reorder(selectedReorderKey); }
    public String selectedPricingKey() { return selectedPricingKey; }
    public String selectedReorderKey() { return selectedReorderKey; }
}