package com.stockpulse.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductMutationConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    private ExecutorService executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void concurrentOrdersNeverCreateNegativeStockOrPartialVelocityUpdates() throws Exception {
        String productId = "PRD-CONCURRENT";
        productRepository.saveAndFlush(new Product(productId, "SKU-CONCURRENT", "Concurrent Product",
                Category.HOME, new BigDecimal("10.00"), 3, 3, 0, ProductStatus.ACTIVE));

        int attempts = 10;
        executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int index = 0; index < attempts; index++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    productService.placeOrder(productId, new OrderRequest(1));
                    return true;
                } catch (ProductConflictException exception) {
                    return false;
                }
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int successfulOrders = 0;
        int conflicts = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get(20, TimeUnit.SECONDS)) {
                    successfulOrders++;
                } else {
                    conflicts++;
                }
            } catch (ExecutionException exception) {
                throw new AssertionError("Concurrent order failed unexpectedly", exception.getCause());
            }
        }

        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(successfulOrders).isEqualTo(3);
        assertThat(conflicts).isEqualTo(7);
        assertThat(finalProduct.getStockLevel()).isZero();
        assertThat(finalProduct.getDemandVelocity()).isEqualTo(successfulOrders);
        assertThat(finalProduct.getStockLevel()).isGreaterThanOrEqualTo(0);
    }
}