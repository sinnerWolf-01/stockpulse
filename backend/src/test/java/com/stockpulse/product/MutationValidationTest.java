package com.stockpulse.product;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MutationValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsOmittedRequiredNumericCreateFields() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-NEW", "Product", Category.HOME, new BigDecimal("10.00"), null, null);

        assertThat(validator.validate(request)).extracting("propertyPath")
                .extracting(Object::toString)
                .contains("stockLevel", "reorderThreshold");
    }

    @Test
    void rejectsOmittedStockPatchAndPricesWithTooManyDecimals() {
        StockPatchRequest missingStock = new StockPatchRequest(null, 0L);
        CreateProductRequest excessivePrice = new CreateProductRequest(
                "SKU-NEW", "Product", Category.HOME, new BigDecimal("10.999"), 1, 1);

        assertThat(validator.validate(missingStock)).extracting("propertyPath")
                .extracting(Object::toString).contains("stockLevel");
        assertThat(validator.validate(excessivePrice)).extracting("propertyPath")
                .extracting(Object::toString).contains("currentPrice");
    }
}