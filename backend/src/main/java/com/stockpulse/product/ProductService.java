package com.stockpulse.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.stockpulse.suggestion.PricingSuggestionRepository;
import com.stockpulse.suggestion.SuggestionStatus;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductIdCounterRepository counterRepository;
    private final ProductStatusPolicy statusPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final PricingSuggestionRepository pricingSuggestionRepository;

    public ProductService(ProductRepository productRepository,
                          ProductIdCounterRepository counterRepository,
                          ProductStatusPolicy statusPolicy,
                           ApplicationEventPublisher eventPublisher,
                           PricingSuggestionRepository pricingSuggestionRepository) {
        this.productRepository = productRepository;
        this.counterRepository = counterRepository;
        this.statusPolicy = statusPolicy;
        this.eventPublisher = eventPublisher;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findProducts(ProductStatus status, Category category) {
        List<Product> products;
        if (status != null && category != null) {
            products = productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            products = productRepository.findByStatus(status);
        } else if (category != null) {
            products = productRepository.findByCategory(category);
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ProductConflictException("SKU already exists: " + request.sku());
        }

        ProductIdCounter counter = counterRepository.findById("PRODUCT")
                .orElseThrow(() -> new IllegalStateException("Product ID counter is not initialized"));
        String id = "PRD-%03d".formatted(counter.allocate());
        Product product = new Product(id, request.sku(), request.name(), request.category(),
                request.currentPrice(), request.stockLevel(), request.reorderThreshold(), 0,
                statusPolicy.derive(request.stockLevel(), false));
        try {
            // Flush here so a concurrent unique-SKU race is translated before the transaction exits.
            Product saved = productRepository.saveAndFlush(product);
            eventPublisher.publishEvent(toEvent(saved, StockSignalSource.PRODUCT_CREATED));
            return ProductResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ProductConflictException("SKU already exists: " + request.sku());
        }
    }

    @Transactional
    public ProductResponse patchStock(String productId, StockPatchRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getVersion().equals(request.expectedVersion())) {
            throw new ProductConflictException("Product was modified by another request: " + productId);
        }
        product.setStockLevel(request.stockLevel());
        product.setStatus(statusPolicy.derive(request.stockLevel(), hasPendingPricing(productId)));
        try {
            Product saved = productRepository.saveAndFlush(product);
            eventPublisher.publishEvent(toEvent(saved, StockSignalSource.MANUAL_STOCK_PATCH));
            return ProductResponse.from(saved);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
            throw new ProductConflictException("Product was modified by another request: " + productId);
        }
    }

    @Transactional
    public ProductResponse placeOrder(String productId, OrderRequest request) {
        int quantity = request == null ? 1 : request.resolvedQuantity();
        if (quantity <= 0) {
            throw new ProductConflictException("Order quantity must be positive");
        }
        productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        int updated = productRepository.decrementStockAndIncrementVelocity(productId, quantity);
        if (updated == 0) {
            throw new ProductConflictException("Insufficient stock for product: " + productId);
        }
        Product saved = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        saved.setStatus(statusPolicy.derive(saved.getStockLevel(), hasPendingPricing(productId)));
        try {
            Product refreshed = productRepository.saveAndFlush(saved);
            eventPublisher.publishEvent(toEvent(refreshed, StockSignalSource.CUSTOMER_ORDER));
            return ProductResponse.from(refreshed);
        } catch (org.springframework.dao.OptimisticLockingFailureException exception) {
            // The transaction rolls back the guarded decrement and velocity increment together.
            throw new ProductConflictException("Product was modified by another request: " + productId);
        }
    }

    private StockSignalEvent toEvent(Product product, StockSignalSource source) {
        return new StockSignalEvent(product.getId(), product.getStockLevel(), product.getReorderThreshold(),
                product.getDemandVelocity(), source);
    }

    private boolean hasPendingPricing(String productId) {
        List<?> pending = pricingSuggestionRepository.findByProductIdAndStatus(productId, SuggestionStatus.PENDING);
        return pending != null && !pending.isEmpty();
    }
}