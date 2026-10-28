package com.stockpulse.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.stockpulse.suggestion.ManualSuggestionService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ManualSuggestionService manualSuggestionService;

    @Test
    void returnsProducts() throws Exception {
        ProductResponse response = new ProductResponse(
                "PRD-001", "SKU-001", "Test Product", Category.ELECTRONICS,
                new BigDecimal("19.99"), 10, 5, 2, ProductStatus.ACTIVE, 0L);
        when(productService.findProducts(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("PRD-001"))
                .andExpect(jsonPath("$[0].currentPrice").value(19.99));
    }
}