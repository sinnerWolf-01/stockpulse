package com.stockpulse.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.stockpulse.suggestion.PricingSuggestionRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductIdCounterRepository counterRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PricingSuggestionRepository pricingSuggestionRepository;

    private ProductService service() {
        return new ProductService(productRepository, counterRepository, new ProductStatusPolicy(), eventPublisher, pricingSuggestionRepository);
    }

    @Test
    void createsProductWithNextConcurrencySafeCounterValueAndPublishesEvent() {
        ProductIdCounter counter = new ProductIdCounter("PRODUCT", 9);
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(counterRepository.findById("PRODUCT")).thenReturn(Optional.of(counter));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service().createProduct(new CreateProductRequest(
                "SKU-NEW", "New Product", Category.HOME, new BigDecimal("12.50"), 3, 5));

        assertThat(response.id()).isEqualTo("PRD-009");
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
        verify(eventPublisher).publishEvent(any(StockSignalEvent.class));
    }

    @Test
    void rejectsDuplicateSku() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service().createProduct(new CreateProductRequest(
                "SKU-001", "Duplicate", Category.HOME, new BigDecimal("12.50"), 3, 5)))
                .isInstanceOf(ProductConflictException.class);
    }

    @Test
    void translatesDatabaseDuplicateSkuConflict() {
        ProductIdCounter counter = new ProductIdCounter("PRODUCT", 9);
        when(productRepository.existsBySku("SKU-RACE")).thenReturn(false);
        when(counterRepository.findById("PRODUCT")).thenReturn(Optional.of(counter));
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate SKU"));

        assertThatThrownBy(() -> service().createProduct(new CreateProductRequest(
                "SKU-RACE", "Race", Category.HOME, new BigDecimal("12.50"), 3, 5)))
                .isInstanceOf(ProductConflictException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    void defaultsOrderQuantityToOneAndPublishesCustomerOrder() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn("PRD-001");
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(productRepository.decrementStockAndIncrementVelocity("PRD-001", 1)).thenReturn(1);
        when(product.getStockLevel()).thenReturn(4);
        when(product.getReorderThreshold()).thenReturn(5);
        when(product.getDemandVelocity()).thenReturn(2);
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        service().placeOrder("PRD-001", new OrderRequest(null));

        verify(productRepository).decrementStockAndIncrementVelocity("PRD-001", 1);
        ArgumentCaptor<StockSignalEvent> event = ArgumentCaptor.forClass(StockSignalEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().source()).isEqualTo(StockSignalSource.CUSTOMER_ORDER);
    }

    @Test
    void rejectsInsufficientStockWithoutPublishingEvent() {
        Product product = mock(Product.class);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(productRepository.decrementStockAndIncrementVelocity("PRD-001", 2)).thenReturn(0);

        assertThatThrownBy(() -> service().placeOrder("PRD-001", new OrderRequest(2)))
                .isInstanceOf(ProductConflictException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void translatesOrderOptimisticLockConflict() {
        Product product = mock(Product.class);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(productRepository.decrementStockAndIncrementVelocity("PRD-001", 1)).thenReturn(1);
        when(product.getStockLevel()).thenReturn(4);
        when(productRepository.saveAndFlush(product))
                .thenThrow(new ObjectOptimisticLockingFailureException(Product.class, "PRD-001"));

        assertThatThrownBy(() -> service().placeOrder("PRD-001", new OrderRequest(1)))
                .isInstanceOf(ProductConflictException.class)
                .hasMessageContaining("modified");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void missingProductsReturnNotFoundForBothMutations() {
        when(productRepository.findById("PRD-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().patchStock("PRD-404", new StockPatchRequest(2, 0L)))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service().placeOrder("PRD-404", new OrderRequest(1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void stockPatchChangesStockAndStatusWithoutChangingVelocity() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn("PRD-001");
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(product.getVersion()).thenReturn(0L);
        when(productRepository.saveAndFlush(product)).thenReturn(product);
        when(product.getStockLevel()).thenReturn(0);
        when(product.getReorderThreshold()).thenReturn(5);
        when(product.getDemandVelocity()).thenReturn(7);

        service().patchStock("PRD-001", new StockPatchRequest(0, 0L));

        verify(product).setStockLevel(0);
        verify(product).setStatus(ProductStatus.OUT_OF_STOCK);
        verify(eventPublisher).publishEvent(any(StockSignalEvent.class));
    }

    @Test
    void doesNotPublishEventWhenOrderTransactionFailsBeforeCompletion() {
        Product product = mock(Product.class);
        when(productRepository.findById("PRD-001")).thenReturn(Optional.of(product));
        when(productRepository.decrementStockAndIncrementVelocity("PRD-001", 1)).thenReturn(0);

        assertThatThrownBy(() -> service().placeOrder("PRD-001", new OrderRequest(1)))
                .isInstanceOf(ProductConflictException.class);

        verify(eventPublisher, never()).publishEvent(any(StockSignalEvent.class));
    }

    @Test
    void stockSignalEventDefinesEvaluationExclusionForReorderAcceptance() {
        StockSignalEvent customerOrder = new StockSignalEvent("PRD-001", 4, 5, 2,
                StockSignalSource.CUSTOMER_ORDER);
        StockSignalEvent reorderAcceptance = new StockSignalEvent("PRD-001", 9, 5, 2,
                StockSignalSource.REORDER_ACCEPTANCE);

        assertThat(customerOrder.triggersEvaluation()).isTrue();
        assertThat(reorderAcceptance.triggersEvaluation()).isFalse();
    }

    @Test
    void rejectsInvalidStockSignalSnapshots() {
        assertThatThrownBy(() -> new StockSignalEvent("", 1, 1, 1, StockSignalSource.CUSTOMER_ORDER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StockSignalEvent("PRD-001", -1, 1, 1, StockSignalSource.CUSTOMER_ORDER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StockSignalEvent("PRD-001", 1, 1, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}