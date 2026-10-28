package com.stockpulse.suggestion;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductNotFoundException;
import com.stockpulse.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CommerceAdvisor {
    private static final Logger log = LoggerFactory.getLogger(CommerceAdvisor.class);

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final StrategyRegistry strategyRegistry;
    private final SuggestionPersistenceWriter persistenceWriter;

    public CommerceAdvisor(ProductRepository productRepository,
                           PricingSuggestionRepository pricingRepository,
                           ReorderSuggestionRepository reorderRepository,
                           StrategyRegistry strategyRegistry,
                           SuggestionPersistenceWriter persistenceWriter) {
        this.productRepository = productRepository;
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.strategyRegistry = strategyRegistry;
        this.persistenceWriter = persistenceWriter;
    }

    public PricingSuggestion suggestPricing(String productId, TriggerReason triggerReason) {
        return suggestPricing(productId, triggerReason, SuggestionSource.RULE);
    }

    public PricingSuggestion suggestPricing(String productId, TriggerReason triggerReason, SuggestionSource source) {
        Product product = product(productId);
        String activeKey = activeKey(productId, triggerReason);
        var existing = pricingRepository.findByActiveKey(activeKey);
        if (existing.isPresent()) return existing.get();

        PricingRecommendation recommendation;
        SuggestionSource actualSource = source;
        try {
            recommendation = strategyRegistry.selectedPricing().recommend(context(product, triggerReason));
            if ("ai".equals(strategyRegistry.selectedPricingKey()) && source == SuggestionSource.RULE) {
                actualSource = SuggestionSource.AI;
            }
        } catch (RuntimeException exception) {
            if (!"ai".equals(strategyRegistry.selectedPricingKey())) throw exception;
            log.warn("AI pricing failed for {}; using rule fallback", productId, exception);
            recommendation = strategyRegistry.defaultPricing().recommend(context(product, triggerReason));
            actualSource = SuggestionSource.RULE_FALLBACK;
        }
        PricingSuggestion suggestion = new PricingSuggestion(productId, product.getCurrentPrice(),
                recommendation.recommendedPrice(), recommendation.direction(), triggerReason, actualSource,
                recommendation.confidence(), recommendation.reasoning(), activeKey);
        try {
            return persistenceWriter.savePricing(suggestion);
        } catch (RuntimeException exception) {
            return pricingRepository.findByActiveKey(activeKey).orElseThrow(() -> exception);
        }
    }

    public ReorderSuggestion suggestReorder(String productId, TriggerReason triggerReason) {
        return suggestReorder(productId, triggerReason, SuggestionSource.RULE);
    }

    public ReorderSuggestion suggestReorder(String productId, TriggerReason triggerReason, SuggestionSource source) {
        Product product = product(productId);
        String activeKey = activeKey(productId, triggerReason);
        var existing = reorderRepository.findByActiveKey(activeKey);
        if (existing.isPresent()) return existing.get();

        ReorderRecommendation recommendation;
        SuggestionSource actualSource = source;
        try {
            recommendation = strategyRegistry.selectedReorder().recommend(context(product, triggerReason));
            if ("ai".equals(strategyRegistry.selectedReorderKey()) && source == SuggestionSource.RULE) {
                actualSource = SuggestionSource.AI;
            }
        } catch (RuntimeException exception) {
            if (!"ai".equals(strategyRegistry.selectedReorderKey())) throw exception;
            log.warn("AI reorder failed for {}; using rule fallback", productId, exception);
            recommendation = strategyRegistry.defaultReorder().recommend(context(product, triggerReason));
            actualSource = SuggestionSource.RULE_FALLBACK;
        }
        ReorderSuggestion suggestion = new ReorderSuggestion(productId, recommendation.recommendedQuantity(),
                recommendation.leadTimeDays(), triggerReason, actualSource, recommendation.confidence(),
                recommendation.reasoning(), activeKey);
        try {
            return persistenceWriter.saveReorder(suggestion);
        } catch (RuntimeException exception) {
            return reorderRepository.findByActiveKey(activeKey).orElseThrow(() -> exception);
        }
    }

    private SignalContext context(Product product, TriggerReason triggerReason) {
        List<Product> peers = productRepository.findByCategory(product.getCategory()).stream()
                .filter(peer -> !peer.getId().equals(product.getId()))
                .toList();
        BigDecimal peerAverage = peers.stream()
                .map(peer -> BigDecimal.valueOf(peer.getDemandVelocity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long peerCount = peers.size();
        if (peerCount > 0) peerAverage = peerAverage.divide(BigDecimal.valueOf(peerCount), 4, RoundingMode.HALF_UP);
        return new SignalContext(product.getId(), product.getCategory(), product.getCurrentPrice(), product.getStockLevel(),
                product.getReorderThreshold(), product.getDemandVelocity(), peerAverage, triggerReason);
    }

    private Product product(String productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private String activeKey(String productId, TriggerReason triggerReason) {
        if (triggerReason == null) throw new IllegalArgumentException("triggerReason is required");
        return productId + "|" + triggerReason;
    }
}