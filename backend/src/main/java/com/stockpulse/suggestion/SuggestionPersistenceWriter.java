package com.stockpulse.suggestion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuggestionPersistenceWriter {

    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;

    public SuggestionPersistenceWriter(PricingSuggestionRepository pricingRepository,
                                       ReorderSuggestionRepository reorderRepository) {
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PricingSuggestion savePricing(PricingSuggestion suggestion) {
        return pricingRepository.saveAndFlush(suggestion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReorderSuggestion saveReorder(ReorderSuggestion suggestion) {
        return reorderRepository.saveAndFlush(suggestion);
    }
}