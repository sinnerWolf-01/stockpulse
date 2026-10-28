package com.stockpulse.suggestion;

import com.stockpulse.product.StockSignalEvent;
import com.stockpulse.product.StockSignalSource;
import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSignalEvaluationServiceTest {

    @Mock CommerceAdvisor commerceAdvisor;
    @Mock ProductRepository productRepository;

    @Test
    void evaluatesInventoryAndDemandSignalsIndependently() {
        StockSignalEvaluationService service = new StockSignalEvaluationService(commerceAdvisor);
        StockSignalEvent event = new StockSignalEvent("PRD-001", 2, 5, 3, StockSignalSource.CUSTOMER_ORDER);

        service.evaluate(event);

        InOrder order = inOrder(commerceAdvisor);
        order.verify(commerceAdvisor).suggestPricing("PRD-001", TriggerReason.INVENTORY_LOW);
        order.verify(commerceAdvisor).suggestReorder("PRD-001", TriggerReason.INVENTORY_LOW);
        order.verify(commerceAdvisor).suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);
        order.verify(commerceAdvisor).suggestReorder("PRD-001", TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void evaluatesOnlyDemandWhenStockIsNotLow() {
        StockSignalEvaluationService service = new StockSignalEvaluationService(commerceAdvisor);

        service.evaluate(new StockSignalEvent("PRD-001", 8, 5, 3, StockSignalSource.MANUAL_STOCK_PATCH));

        verify(commerceAdvisor).suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);
        verify(commerceAdvisor).suggestReorder("PRD-001", TriggerReason.DEMAND_SPIKE);
        verify(commerceAdvisor, never()).suggestPricing("PRD-001", TriggerReason.INVENTORY_LOW);
        verify(commerceAdvisor, never()).suggestReorder("PRD-001", TriggerReason.INVENTORY_LOW);
    }

    @Test
    void skipsReorderAcceptanceWithoutCallingAdvisor() {
        StockSignalEvaluationService service = new StockSignalEvaluationService(commerceAdvisor);

        service.evaluate(new StockSignalEvent("PRD-001", 0, 5, 3, StockSignalSource.REORDER_ACCEPTANCE));

        verify(commerceAdvisor, never()).suggestPricing("PRD-001", TriggerReason.INVENTORY_LOW);
        verify(commerceAdvisor, never()).suggestReorder("PRD-001", TriggerReason.INVENTORY_LOW);
        verify(commerceAdvisor, never()).suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);
        verify(commerceAdvisor, never()).suggestReorder("PRD-001", TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void requiresVelocityToExceedTwiceThePeerAverageForDemandSpike() {
        Product product = product("PRD-001", 5);
        Product peer = product("PRD-002", 2);
        when(productRepository.findById("PRD-001")).thenReturn(java.util.Optional.of(product));
        when(productRepository.findByCategory(Category.HOME)).thenReturn(java.util.List.of(product, peer));

        StockSignalEvaluationService service = new StockSignalEvaluationService(
                commerceAdvisor, productRepository, new java.math.BigDecimal("2.0"), 1);
        service.evaluate(new StockSignalEvent("PRD-001", 8, 5, 5, StockSignalSource.CUSTOMER_ORDER));

        verify(commerceAdvisor).suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);
        verify(commerceAdvisor).suggestReorder("PRD-001", TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void doesNotCreateDemandSignalWhenVelocityDoesNotExceedPeerThreshold() {
        Product product = product("PRD-001", 4);
        Product peer = product("PRD-002", 2);
        when(productRepository.findById("PRD-001")).thenReturn(java.util.Optional.of(product));
        when(productRepository.findByCategory(Category.HOME)).thenReturn(java.util.List.of(product, peer));

        StockSignalEvaluationService service = new StockSignalEvaluationService(
                commerceAdvisor, productRepository, new java.math.BigDecimal("2.0"), 1);
        service.evaluate(new StockSignalEvent("PRD-001", 8, 5, 4, StockSignalSource.CUSTOMER_ORDER));

        verify(commerceAdvisor, never()).suggestPricing("PRD-001", TriggerReason.DEMAND_SPIKE);
        verify(commerceAdvisor, never()).suggestReorder("PRD-001", TriggerReason.DEMAND_SPIKE);
    }

    private Product product(String id, int velocity) {
        Product product = new Product(id, id + "-SKU", id, Category.HOME,
                new java.math.BigDecimal("10.00"), 8, 5, velocity,
                com.stockpulse.product.ProductStatus.ACTIVE);
        return product;
    }
}