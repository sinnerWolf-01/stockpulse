package com.stockpulse.suggestion;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductAcceptanceOperations;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import com.stockpulse.product.StockSignalEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock PricingSuggestionRepository pricingRepository;
    @Mock ReorderSuggestionRepository reorderRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductAcceptanceOperations productOperations;
    @Mock ApplicationEventPublisher eventPublisher;

    private SuggestionService service() {
        return new SuggestionService(pricingRepository, reorderRepository, productRepository,
                productOperations, eventPublisher);
    }

    @Test
    void acceptsPricingAppliesPriceAndSupersedesOtherPendingSuggestions() {
        Product product = new Product("PRD-001", "SKU-1", "Product", com.stockpulse.product.Category.HOME,
                new BigDecimal("10.00"), 5, 5, 1, ProductStatus.ACTIVE);
        PricingSuggestion accepted = new PricingSuggestion("PRD-001", new BigDecimal("10.00"),
                new BigDecimal("11.00"), Direction.INCREASE, TriggerReason.INVENTORY_LOW,
                SuggestionSource.RULE, new BigDecimal("0.6000"), "Low stock", "PRD-001|INVENTORY_LOW");
        PricingSuggestion other = new PricingSuggestion("PRD-001", new BigDecimal("10.00"),
                new BigDecimal("10.50"), Direction.INCREASE, TriggerReason.DEMAND_SPIKE,
                SuggestionSource.RULE, new BigDecimal("0.6000"), "Demand", "PRD-001|DEMAND_SPIKE");
        when(pricingRepository.findById(1L)).thenReturn(Optional.of(accepted));
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(pricingRepository.findByProductIdAndStatus("PRD-001", SuggestionStatus.PENDING))
                .thenReturn(List.of(accepted, other), List.of());

        PricingSuggestionDto result = service().acceptPricing(1L, new SuggestionResolutionRequest(null, SuggestionStatus.ACCEPTED, "Approved"));

        assertThat(result.status()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(other.getStatus()).isEqualTo(SuggestionStatus.REJECTED);
        verify(productOperations).applyAcceptedPrice(product, new BigDecimal("11.00"), false);
    }

    @Test
    void rejectsStalePricingBeforeResolvingSuggestion() {
        Product product = new Product("PRD-001", "SKU-1", "Product", com.stockpulse.product.Category.HOME,
                new BigDecimal("10.00"), 5, 5, 1, ProductStatus.ACTIVE);
        PricingSuggestion suggestion = new PricingSuggestion("PRD-001", new BigDecimal("10.00"),
                new BigDecimal("11.00"), Direction.INCREASE, TriggerReason.INVENTORY_LOW,
                SuggestionSource.RULE, new BigDecimal("0.6000"), "Low stock", "PRD-001|INVENTORY_LOW");
        when(pricingRepository.findById(1L)).thenReturn(Optional.of(suggestion));
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service().acceptPricing(1L, new SuggestionResolutionRequest(99L, SuggestionStatus.ACCEPTED, null)))
                .isInstanceOf(com.stockpulse.product.ProductConflictException.class);
        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.PENDING);
    }

    @Test
    void acceptsReorderUpdatesStockAndPublishesNonRecursiveEvent() {
        Product product = new Product("PRD-001", "SKU-1", "Product", com.stockpulse.product.Category.HOME,
                new BigDecimal("10.00"), 2, 5, 1, ProductStatus.ACTIVE);
        ReorderSuggestion suggestion = new ReorderSuggestion("PRD-001", 8, 7, TriggerReason.INVENTORY_LOW,
                SuggestionSource.RULE, new BigDecimal("0.6000"), "Restore stock", "PRD-001|INVENTORY_LOW");
        when(reorderRepository.findById(1L)).thenReturn(Optional.of(suggestion));
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(reorderRepository.findByProductIdAndStatus("PRD-001", SuggestionStatus.PENDING))
                .thenReturn(List.of(suggestion));

        service().acceptReorder(1L, new SuggestionResolutionRequest(null, SuggestionStatus.ACCEPTED, "Ordered"));

        verify(productOperations).applyAcceptedReorder(product, 8, false);
        ArgumentCaptor<StockSignalEvent> event = ArgumentCaptor.forClass(StockSignalEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().source()).isEqualTo(com.stockpulse.product.StockSignalSource.REORDER_ACCEPTANCE);
        assertThat(event.getValue().triggersEvaluation()).isFalse();
    }
}