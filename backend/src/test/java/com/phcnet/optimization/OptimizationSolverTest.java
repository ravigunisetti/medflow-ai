package com.phcnet.optimization;

import com.phcnet.optimization.dto.OptimizationRequest;
import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import com.phcnet.optimization.service.OptimizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OptimizationSolverTest {

    @Autowired
    private OptimizationService optimizationService;

    @Test
    void testDeterministicRedistributionSolver() {
        OptimizationRequest req = new OptimizationRequest(null, null, 100.0, 14.0);
        List<RedistributionRecommendationDTO> recommendations = optimizationService.calculateRedistributions(req);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "Expected solver to find redistribution matches between surplus and deficit PHCs");

        for (RedistributionRecommendationDTO rec : recommendations) {
            // Assert hard constraints
            assertNotEquals(rec.sourcePhcId(), rec.destinationPhcId(), "Source and destination PHC cannot be identical");
            assertTrue(rec.quantity() > 0, "Transfer quantity must be strictly positive");
            assertTrue(rec.distanceKm() <= 100.0, "Distance must respect maximum threshold constraint");
            assertTrue(rec.estimatedCost() > 0, "Logistics cost must be positive");
            assertTrue(rec.projectedDaysAfter() > rec.projectedDaysBefore(), "Post-transfer stock days must increase for destination");
            assertNotNull(rec.reason());
        }
    }
}
