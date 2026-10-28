package com.stockpulse.suggestion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/strategy")
public class StrategyController {
    private final StrategyRegistry registry;

    public StrategyController(StrategyRegistry registry) { this.registry = registry; }

    @GetMapping
    public Map<String, String> current() {
        return Map.of("pricing", registry.selectedPricingKey(), "reorder", registry.selectedReorderKey());
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> update(@RequestBody StrategySelectionRequest request) {
        if (request.pricing() == null && request.reorder() == null) {
            throw new IllegalArgumentException("At least one strategy key is required");
        }
        registry.select(request.pricing(), request.reorder());
        return ResponseEntity.ok(current());
    }

    public record StrategySelectionRequest(String pricing, String reorder) { }
}