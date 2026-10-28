package com.stockpulse.suggestion;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductAcceptanceOperations;
import com.stockpulse.product.ProductNotFoundException;
import com.stockpulse.product.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ManualSuggestionService {

    private final CommerceAdvisor commerceAdvisor;
    private final ProductRepository productRepository;
    private final ProductAcceptanceOperations productOperations;

    public ManualSuggestionService(CommerceAdvisor commerceAdvisor,
                                   ProductRepository productRepository,
                                   ProductAcceptanceOperations productOperations) {
        this.commerceAdvisor = commerceAdvisor;
        this.productRepository = productRepository;
        this.productOperations = productOperations;
    }

    public PricingSuggestionDto suggestPricing(String productId) {
        PricingSuggestion suggestion = commerceAdvisor.suggestPricing(productId, TriggerReason.MANUAL, SuggestionSource.MANUAL);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        productOperations.markPricingReviewPending(product);
        productRepository.saveAndFlush(product);
        return PricingSuggestionDto.from(suggestion);
    }

    public ReorderSuggestionDto suggestReorder(String productId) {
        ReorderSuggestion suggestion = commerceAdvisor.suggestReorder(productId, TriggerReason.MANUAL, SuggestionSource.MANUAL);
        return ReorderSuggestionDto.from(suggestion);
    }
}