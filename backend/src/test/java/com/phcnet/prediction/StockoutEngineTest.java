package com.phcnet.prediction;

import com.phcnet.prediction.dto.RiskExplanationDTO;
import com.phcnet.prediction.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockoutEngineTest {

    @Autowired
    private PredictionService predictionService;

    @Test
    void testDeterministicRiskExplanation() {
        // PHC 1 and Medicine 5 (Paracetamol) was seeded with critical/deficit stock
        RiskExplanationDTO explanation = predictionService.explainRisk(1L, 5L);

        assertNotNull(explanation);
        assertEquals(1L, explanation.phcId());
        assertEquals(5L, explanation.medicineId());
        assertTrue(explanation.currentStock() >= 0);
        assertTrue(explanation.averageDailyDemand() > 0);
        assertTrue(explanation.daysToStockout() >= 0);
        assertNotNull(explanation.riskLevel());
        assertNotNull(explanation.mathematicalDerivation());
        assertNotNull(explanation.humanReadableExplanation());
        assertTrue(explanation.formulaUsed().contains("current_stock / average_daily_demand"));
    }

    @Test
    void testThresholdClassifications() {
        // Test configured thresholds
        RiskExplanationDTO explanation = predictionService.explainRisk(1L, 5L);
        assertEquals(3.0, explanation.criticalThresholdDays());
        assertEquals(7.0, explanation.highThresholdDays());
        assertEquals(14.0, explanation.mediumThresholdDays());
    }
}
