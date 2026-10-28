package com.stockpulse.suggestion;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductAcceptanceOperations;
import com.stockpulse.product.ProductConflictException;
import com.stockpulse.product.ProductNotFoundException;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.StockSignalEvent;
import com.stockpulse.product.StockSignalSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SuggestionService {

    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final ProductRepository productRepository;
    private final ProductAcceptanceOperations productOperations;
    private final ApplicationEventPublisher eventPublisher;

    public SuggestionService(PricingSuggestionRepository pricingRepository,
                             ReorderSuggestionRepository reorderRepository,
                             ProductRepository productRepository,
                             ProductAcceptanceOperations productOperations,
                             ApplicationEventPublisher eventPublisher) {
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.productRepository = productRepository;
        this.productOperations = productOperations;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestionDto> findPricing(SuggestionStatus status) {
        SuggestionStatus requestedStatus = status == null ? SuggestionStatus.PENDING : status;
        List<PricingSuggestion> suggestions =
                pricingRepository.findAll().stream().filter(suggestion -> suggestion.getStatus() == requestedStatus).toList();
        return suggestions.stream().map(PricingSuggestionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDto> findReorder(SuggestionStatus status) {
        SuggestionStatus requestedStatus = status == null ? SuggestionStatus.PENDING : status;
        List<ReorderSuggestion> suggestions =
                reorderRepository.findAll().stream().filter(suggestion -> suggestion.getStatus() == requestedStatus).toList();
        return suggestions.stream().map(ReorderSuggestionDto::from).toList();
    }

    @Transactional
    public PricingSuggestionDto acceptPricing(Long id, SuggestionResolutionRequest request) {
        PricingSuggestion suggestion = pricingRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException("Pricing", id));
        Product product = product(suggestion.getProductId());
        verifyVersion(product, request.expectedProductVersion());
        if (product.getCurrentPrice().compareTo(suggestion.getCurrentPrice()) != 0) {
            throw new ProductConflictException("Product price changed since suggestion was created: " + product.getId());
        }
        resolve(suggestion, SuggestionStatus.ACCEPTED, request.note());
        supersedePricing(suggestion);
        productOperations.applyAcceptedPrice(product, suggestion.getRecommendedPrice(), false);
        return PricingSuggestionDto.from(suggestion);
    }

    @Transactional
    public PricingSuggestionDto rejectPricing(Long id, SuggestionResolutionRequest request) {
        PricingSuggestion suggestion = pricingRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException("Pricing", id));
        Product product = product(suggestion.getProductId());
        verifyVersion(product, request.expectedProductVersion());
        resolve(suggestion, SuggestionStatus.REJECTED, request.note());
        productOperations.deriveStatus(product, hasPendingPricing(product.getId()));
        return PricingSuggestionDto.from(suggestion);
    }

    @Transactional
    public ReorderSuggestionDto acceptReorder(Long id, SuggestionResolutionRequest request) {
        ReorderSuggestion suggestion = reorderRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException("Reorder", id));
        Product product = product(suggestion.getProductId());
        verifyVersion(product, request.expectedProductVersion());
        resolve(suggestion, SuggestionStatus.ACCEPTED, request.note());
        supersedeReorder(suggestion);
        productOperations.applyAcceptedReorder(product, suggestion.getRecommendedQuantity(), hasPendingPricing(product.getId()));
        eventPublisher.publishEvent(new StockSignalEvent(product.getId(), product.getStockLevel(), product.getReorderThreshold(),
                product.getDemandVelocity(), StockSignalSource.REORDER_ACCEPTANCE));
        return ReorderSuggestionDto.from(suggestion);
    }

    @Transactional
    public ReorderSuggestionDto rejectReorder(Long id, SuggestionResolutionRequest request) {
        ReorderSuggestion suggestion = reorderRepository.findById(id)
                .orElseThrow(() -> new SuggestionNotFoundException("Reorder", id));
        Product product = product(suggestion.getProductId());
        verifyVersion(product, request.expectedProductVersion());
        resolve(suggestion, SuggestionStatus.REJECTED, request.note());
        productOperations.deriveStatus(product, hasPendingPricing(product.getId()));
        return ReorderSuggestionDto.from(suggestion);
    }

    private void supersedePricing(PricingSuggestion accepted) {
        pricingRepository.findByProductIdAndStatus(accepted.getProductId(), SuggestionStatus.PENDING).stream()
                .filter(other -> other != accepted)
                .forEach(other -> resolve(other, SuggestionStatus.REJECTED, "Superseded by accepted pricing suggestion"));
    }

    private void supersedeReorder(ReorderSuggestion accepted) {
        reorderRepository.findByProductIdAndStatus(accepted.getProductId(), SuggestionStatus.PENDING).stream()
                .filter(other -> other != accepted)
                .forEach(other -> resolve(other, SuggestionStatus.REJECTED, "Superseded by accepted reorder suggestion"));
    }

    private boolean hasPendingPricing(String productId) {
        return pricingRepository.findByProductIdAndStatus(productId, SuggestionStatus.PENDING).stream().findAny().isPresent();
    }

    private Product product(String productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void verifyVersion(Product product, Long expectedVersion) {
        if (!java.util.Objects.equals(product.getVersion(), expectedVersion)) {
            throw new ProductConflictException("Product was modified by another request: " + product.getId());
        }
    }

    private void resolve(PricingSuggestion suggestion, SuggestionStatus status, String note) {
        try {
            suggestion.resolve(status, note);
        } catch (IllegalStateException exception) {
            throw new ProductConflictException("Suggestion is no longer pending: " + suggestion.getId());
        }
    }

    private void resolve(ReorderSuggestion suggestion, SuggestionStatus status, String note) {
        try {
            suggestion.resolve(status, note);
        } catch (IllegalStateException exception) {
            throw new ProductConflictException("Suggestion is no longer pending: " + suggestion.getId());
        }
    }
}