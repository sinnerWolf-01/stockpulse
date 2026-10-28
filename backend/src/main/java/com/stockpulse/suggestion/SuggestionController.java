package com.stockpulse.suggestion;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping({"", "/suggestions"})
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping({"/pricing", "/pricing-suggestions"})
    public ResponseEntity<List<PricingSuggestionDto>> pricing(@RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.findPricing(status));
    }

    @GetMapping({"/reorder", "/reorder-suggestions"})
    public ResponseEntity<List<ReorderSuggestionDto>> reorder(@RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.findReorder(status));
    }

    @PostMapping("/pricing/{id}/accept")
    public ResponseEntity<PricingSuggestionDto> acceptPricing(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        return ResponseEntity.ok(suggestionService.acceptPricing(id, request));
    }

    @PostMapping("/pricing/{id}/reject")
    public ResponseEntity<PricingSuggestionDto> rejectPricing(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        return ResponseEntity.ok(suggestionService.rejectPricing(id, request));
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public ResponseEntity<PricingSuggestionDto> resolvePricing(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        if (request.status() == SuggestionStatus.ACCEPTED) return acceptPricing(id, request);
        if (request.status() == SuggestionStatus.REJECTED) return rejectPricing(id, request);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution status must be ACCEPTED or REJECTED");
    }

    @PostMapping("/reorder/{id}/accept")
    public ResponseEntity<ReorderSuggestionDto> acceptReorder(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        return ResponseEntity.ok(suggestionService.acceptReorder(id, request));
    }

    @PostMapping("/reorder/{id}/reject")
    public ResponseEntity<ReorderSuggestionDto> rejectReorder(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        return ResponseEntity.ok(suggestionService.rejectReorder(id, request));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ResponseEntity<ReorderSuggestionDto> resolveReorder(@PathVariable Long id, @Valid @RequestBody SuggestionResolutionRequest request) {
        if (request.status() == SuggestionStatus.ACCEPTED) return acceptReorder(id, request);
        if (request.status() == SuggestionStatus.REJECTED) return rejectReorder(id, request);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution status must be ACCEPTED or REJECTED");
    }
}