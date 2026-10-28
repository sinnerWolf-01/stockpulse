package com.stockpulse.config;

import com.stockpulse.product.Category;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductIdCounter;
import com.stockpulse.product.ProductIdCounterRepository;
import com.stockpulse.product.ProductStatusPolicy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository,
                                   ProductIdCounterRepository counterRepository,
                                   ProductStatusPolicy productStatusPolicy) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            productRepository.saveAll(List.of(
                    product("PRD-001", "SKU-ELEC-001", "Wireless Earbuds Pro", Category.ELECTRONICS, "79.99", 45, 20, 3, productStatusPolicy),
                    product("PRD-002", "SKU-ELEC-002", "USB-C Hub 7-Port", Category.ELECTRONICS, "34.99", 120, 30, 1, productStatusPolicy),
                    // This product is intentionally ACTIVE even though it is below threshold.
                    // Status is derived from mutations; it becomes review-pending after a signal creates a suggestion.
                    product("PRD-003", "SKU-APP-001", "Organic Cotton T-Shirt", Category.APPAREL, "24.99", 8, 15, 12, productStatusPolicy),
                    product("PRD-004", "SKU-APP-002", "Running Shorts - Navy", Category.APPAREL, "39.99", 55, 20, 2, productStatusPolicy),
                    product("PRD-005", "SKU-HOME-001", "Ceramic Pour-Over Set", Category.HOME, "49.99", 22, 10, 4, productStatusPolicy),
                    product("PRD-006", "SKU-HOME-002", "LED Desk Lamp - Dimmable", Category.HOME, "59.99", 0, 15, 0, productStatusPolicy),
                    product("PRD-007", "SKU-ELEC-003", "Portable Charger 20K", Category.ELECTRONICS, "44.99", 18, 25, 8, productStatusPolicy),
                    product("PRD-008", "SKU-APP-003", "Hoodie - Heather Grey", Category.APPAREL, "54.99", 11, 12, 15, productStatusPolicy)
            ));
            // The fixed seed set occupies PRD-001 through PRD-008; allocation therefore starts at PRD-009.
            counterRepository.save(new ProductIdCounter("PRODUCT", 9));
        };
    }

    private static Product product(String id, String sku, String name, Category category,
                                   String price, int stock, int threshold, int velocity,
                                   ProductStatusPolicy productStatusPolicy) {
        // Seed status follows the same policy as future mutations; no pricing suggestion exists at startup.
        return new Product(id, sku, name, category, new BigDecimal(price), stock, threshold, velocity,
                productStatusPolicy.derive(stock, false));
    }
}