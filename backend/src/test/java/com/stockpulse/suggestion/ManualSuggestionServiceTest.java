package com.stockpulse.suggestion;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductAcceptanceOperations;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import com.stockpulse.product.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualSuggestionServiceTest {

    @Mock CommerceAdvisor commerceAdvisor;
    @Mock ProductRepository productRepository;
    @Mock ProductAcceptanceOperations productOperations;

    @Test
    void manualPricingUsesManualSourceAndMarksReviewPending() {
        Product product = new Product("PRD-001", "SKU-1", "Product", Category.HOME,
                new BigDecimal("10.00"), 5, 5, 1, ProductStatus.ACTIVE);
        PricingSuggestion suggestion = new PricingSuggestion("PRD-001", new BigDecimal("10.00"),
                new BigDecimal("11.00"), Direction.INCREASE, TriggerReason.MANUAL, SuggestionSource.MANUAL,
                new BigDecimal("0.6000"), "Manual recommendation", "PRD-001|MANUAL");
        when(commerceAdvisor.suggestPricing("PRD-001", TriggerReason.MANUAL, SuggestionSource.MANUAL)).thenReturn(suggestion);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));

        PricingSuggestionDto result = new ManualSuggestionService(commerceAdvisor, productRepository, productOperations)
                .suggestPricing("PRD-001");

        assertThat(result.source()).isEqualTo(SuggestionSource.MANUAL);
        verify(productOperations).markPricingReviewPending(product);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void manualReorderDelegatesToAdvisorWithManualSource() {
        ReorderSuggestion suggestion = new ReorderSuggestion("PRD-001", 10, 7, TriggerReason.MANUAL,
                SuggestionSource.MANUAL, new BigDecimal("0.6000"), "Manual recommendation", "PRD-001|MANUAL");
        when(commerceAdvisor.suggestReorder("PRD-001", TriggerReason.MANUAL, SuggestionSource.MANUAL)).thenReturn(suggestion);

        ReorderSuggestionDto result = new ManualSuggestionService(commerceAdvisor, productRepository, productOperations)
                .suggestReorder("PRD-001");

        assertThat(result.source()).isEqualTo(SuggestionSource.MANUAL);
        verifyNoInteractions(productRepository, productOperations);
    }
}