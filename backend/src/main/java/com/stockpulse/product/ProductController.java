package com.stockpulse.product;

import com.stockpulse.suggestion.ManualSuggestionService;
import com.stockpulse.suggestion.PricingSuggestionDto;
import com.stockpulse.suggestion.ReorderSuggestionDto;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ManualSuggestionService manualSuggestionService;

    public ProductController(ProductService productService, ManualSuggestionService manualSuggestionService) {
        this.productService = productService;
        this.manualSuggestionService = manualSuggestionService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(productService.findProducts(status, category));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> patchStock(@PathVariable String id,
                                                      @Valid @RequestBody StockPatchRequest request) {
        return ResponseEntity.ok(productService.patchStock(id, request));
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<ProductResponse> placeOrder(@PathVariable String id,
                                                      @Valid @RequestBody(required = false) OrderRequest request) {
        return ResponseEntity.ok(productService.placeOrder(id, request));
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<PricingSuggestionDto> suggestPricing(@PathVariable String id) {
        return ResponseEntity.ok(manualSuggestionService.suggestPricing(id));
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<ReorderSuggestionDto> suggestReorder(@PathVariable String id) {
        return ResponseEntity.ok(manualSuggestionService.suggestReorder(id));
    }
}