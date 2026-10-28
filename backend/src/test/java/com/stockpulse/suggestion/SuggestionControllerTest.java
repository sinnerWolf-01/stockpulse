package com.stockpulse.suggestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestionController.class)
class SuggestionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean SuggestionService suggestionService;

    @Test
    void listsPendingPricingSuggestions() throws Exception {
        when(suggestionService.findPricing(SuggestionStatus.PENDING)).thenReturn(List.of(
                new PricingSuggestionDto(1L, "PRD-001", null, null, Direction.HOLD,
                        TriggerReason.INVENTORY_LOW, SuggestionSource.RULE, SuggestionStatus.PENDING,
                        null, "Low stock", null, null, null, null)));

        mockMvc.perform(get("/suggestions/pricing?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void supportsRootAndCompatibilitySuggestionAliases() throws Exception {
        when(suggestionService.findPricing(null)).thenReturn(List.of());
        when(suggestionService.findReorder(null)).thenReturn(List.of());

        mockMvc.perform(get("/pricing-suggestions"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/suggestions/pricing-suggestions"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/reorder-suggestions"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/suggestions/reorder-suggestions"))
                .andExpect(status().isOk());
    }
}