package com.phcnet.simulation;

import com.phcnet.simulation.dto.SimulationRequestDTO;
import com.phcnet.simulation.dto.SimulationResultDTO;
import com.phcnet.simulation.dto.SimulationScenarioDTO;
import com.phcnet.simulation.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SimulationEngineTest {

    @Autowired
    private SimulationService simulationService;

    @Test
    void testPredefinedScenariosRetrieval() {
        List<SimulationScenarioDTO> scenarios = simulationService.getPredefinedScenarios();
        assertNotNull(scenarios);
        assertFalse(scenarios.isEmpty());
        assertTrue(scenarios.size() >= 4, "Should have at least 4 predefined emergency scenarios");

        boolean hasDengue = scenarios.stream().anyMatch(s -> s.scenarioId().equals("MONSOON_DENGUE"));
        assertTrue(hasDengue, "Should contain monsoon dengue scenario");
    }

    @Test
    void testSimulationExecutionAndMitigation() {
        SimulationRequestDTO request = new SimulationRequestDTO(
                "MONSOON_DENGUE",
                null,
                60, // 60% surge
                120, // 120% acute demand multiplier
                14,  // 14 days supply delay
                "Analgesics & Antipyretics"
        );

        SimulationResultDTO result = simulationService.runSimulation(request);

        assertNotNull(result);
        assertTrue(result.afterCriticalCount() >= result.beforeCriticalCount(),
                "Epidemic surge should increase or keep equal the count of critical stockout facilities");
        assertNotNull(result.scenarioSummary());
        assertTrue(result.executionTimeMs() >= 0);
        assertNotNull(result.vulnerableFacilities());
        assertNotNull(result.recommendedTransfers());

        // Validate vulnerable facility projections
        result.vulnerableFacilities().forEach(v -> {
            assertNotNull(v.phc());
            assertNotNull(v.district());
            assertTrue(v.beforeStockDays() >= v.afterStockDays(),
                    "Crisis stock days must be less than or equal to baseline stock days");
            assertTrue(v.afterStockDays() >= 0.0);
        });
    }
}
