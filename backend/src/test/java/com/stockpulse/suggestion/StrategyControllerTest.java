package com.stockpulse.suggestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebMvcTest(StrategyController.class)
class StrategyControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean StrategyRegistry registry;

    @Test
    void getsAndUpdatesCurrentStrategies() throws Exception {
        org.mockito.Mockito.when(registry.selectedPricingKey()).thenReturn("rule", "ai");
        org.mockito.Mockito.when(registry.selectedReorderKey()).thenReturn("rule", "ai");

        mockMvc.perform(get("/admin/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricing").value("rule"));
        mockMvc.perform(put("/admin/strategy").contentType(APPLICATION_JSON)
                        .content("{\"pricing\":\"ai\",\"reorder\":\"ai\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnknownStrategy() throws Exception {
        doThrow(new IllegalArgumentException("Unknown pricing strategy: unknown"))
                .when(registry).select("unknown", null);

        mockMvc.perform(put("/admin/strategy").contentType(APPLICATION_JSON)
                        .content("{\"pricing\":\"unknown\"}"))
                .andExpect(status().isBadRequest());
    }
}