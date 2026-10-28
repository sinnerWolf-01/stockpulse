package com.stockpulse.suggestion;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductService;
import com.stockpulse.product.ProductStatus;
import com.stockpulse.product.StockPatchRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SuggestionAsyncIntegrationTest {

    private static final String PRODUCT_ID = "PRD-ASYNC-TEST";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PricingSuggestionRepository pricingRepository;

    @Autowired
    private ReorderSuggestionRepository reorderRepository;

    @AfterEach
    void cleanUp() {
        pricingRepository.deleteAll();
        reorderRepository.deleteAll();
        productRepository.deleteById(PRODUCT_ID);
    }

    @Test
    void createsPricingAndReorderSuggestionsAfterCommittedStockSignal() {
        productRepository.saveAndFlush(new Product(PRODUCT_ID, "SKU-ASYNC-TEST", "Async test product",
                Category.HOME, new BigDecimal("20.00"), 10, 5, 0, ProductStatus.ACTIVE));

        Product product = productRepository.findById(PRODUCT_ID).orElseThrow();
        productService.patchStock(PRODUCT_ID, new StockPatchRequest(2, product.getVersion()));

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(pricingRepository.findByProductIdAndTriggerReasonAndStatus(
                    PRODUCT_ID, TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)).isPresent();
            assertThat(reorderRepository.findByProductIdAndTriggerReasonAndStatus(
                    PRODUCT_ID, TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING)).isPresent();
        });
    }

    @Test
    void duplicateCommittedSignalsLeaveOnePendingSuggestionOfEachType() {
        productRepository.saveAndFlush(new Product(PRODUCT_ID, "SKU-ASYNC-DUP", "Async duplicate test product",
                Category.HOME, new BigDecimal("20.00"), 10, 5, 0, ProductStatus.ACTIVE));

        Product first = productRepository.findById(PRODUCT_ID).orElseThrow();
        productService.patchStock(PRODUCT_ID, new StockPatchRequest(2, first.getVersion()));
        Long nextVersion = productRepository.findById(PRODUCT_ID).orElseThrow().getVersion();
        productService.patchStock(PRODUCT_ID,
                new StockPatchRequest(1, nextVersion));

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(pricingRepository.findByProductIdAndStatus(PRODUCT_ID, SuggestionStatus.PENDING)).hasSize(1);
            assertThat(reorderRepository.findByProductIdAndStatus(PRODUCT_ID, SuggestionStatus.PENDING)).hasSize(1);
        });
    }
}