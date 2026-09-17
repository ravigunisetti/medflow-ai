package com.phcnet.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetComprehensiveReport() throws Exception {
        mockMvc.perform(get("/api/analytics/comprehensive-report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.topStockoutDistricts", not(empty())))
                .andExpect(jsonPath("$.data.topDemandedMedicines", not(empty())))
                .andExpect(jsonPath("$.data.seasonalDemandAnalysis", not(empty())))
                .andExpect(jsonPath("$.data.redistributionMetrics", notNullValue()));
    }
}
