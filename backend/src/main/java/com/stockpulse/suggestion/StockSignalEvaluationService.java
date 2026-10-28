package com.stockpulse.suggestion;

import com.stockpulse.product.StockSignalEvent;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
public class StockSignalEvaluationService {

    private final CommerceAdvisor commerceAdvisor;
    private final ProductRepository productRepository;
    private final BigDecimal demandMultiplier;
    private final int minimumDemandVelocity;

    public StockSignalEvaluationService(CommerceAdvisor commerceAdvisor) {
        this(commerceAdvisor, null, new BigDecimal("2.0"), 1);
    }

    @Autowired
    public StockSignalEvaluationService(CommerceAdvisor commerceAdvisor,
                                        ProductRepository productRepository,
                                        @Value("${stockpulse.signals.demand-multiplier:2.0}") BigDecimal demandMultiplier,
                                        @Value("${stockpulse.signals.minimum-demand-velocity:1}") int minimumDemandVelocity) {
        this.commerceAdvisor = commerceAdvisor;
        this.productRepository = productRepository;
        if (demandMultiplier == null || demandMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("demandMultiplier must be positive");
        }
        if (minimumDemandVelocity < 0) {
            throw new IllegalArgumentException("minimumDemandVelocity cannot be negative");
        }
        this.demandMultiplier = demandMultiplier;
        this.minimumDemandVelocity = minimumDemandVelocity;
    }

    public void evaluate(StockSignalEvent event) {
        if (!event.triggersEvaluation()) {
            return;
        }
        if (event.stockLevel() < event.reorderThreshold()) {
            commerceAdvisor.suggestPricing(event.productId(), TriggerReason.INVENTORY_LOW);
            commerceAdvisor.suggestReorder(event.productId(), TriggerReason.INVENTORY_LOW);
        }
        if (isDemandSpike(event)) {
            commerceAdvisor.suggestPricing(event.productId(), TriggerReason.DEMAND_SPIKE);
            commerceAdvisor.suggestReorder(event.productId(), TriggerReason.DEMAND_SPIKE);
        }
    }

    private boolean isDemandSpike(StockSignalEvent event) {
        if (event.demandVelocity() < minimumDemandVelocity || productRepository == null) {
            return productRepository == null && event.demandVelocity() >= minimumDemandVelocity;
        }

        Product product = productRepository.findById(event.productId()).orElse(null);
        if (product == null) {
            return false;
        }

        var peers = productRepository.findByCategory(product.getCategory()).stream()
                .filter(peer -> !Objects.equals(peer.getId(), product.getId()))
                .toList();
        BigDecimal peerAverage = peers.stream()
                .map(peer -> BigDecimal.valueOf(peer.getDemandVelocity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long peerCount = peers.size();
        if (peerCount == 0) {
            return false;
        }
        peerAverage = peerAverage.divide(BigDecimal.valueOf(peerCount), 4, RoundingMode.HALF_UP);
        return peerAverage.signum() > 0
                && BigDecimal.valueOf(event.demandVelocity())
                .compareTo(peerAverage.multiply(demandMultiplier)) > 0;
    }
}