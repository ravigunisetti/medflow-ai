package com.phcnet.simulation.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.simulation.dto.SimulationRequestDTO;
import com.phcnet.simulation.dto.SimulationResultDTO;
import com.phcnet.simulation.dto.SimulationScenarioDTO;
import com.phcnet.simulation.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Emergency Simulation", description = "Epidemic stress-testing and sandbox disaster simulation APIs")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/scenarios")
    @Operation(summary = "List predefined emergency simulation scenarios")
    public ResponseEntity<ApiResponse<List<SimulationScenarioDTO>>> getScenarios() {
        List<SimulationScenarioDTO> scenarios = simulationService.getPredefinedScenarios();
        return ResponseEntity.ok(ApiResponse.ok(scenarios));
    }

    @PostMapping("/run")
    @Operation(summary = "Execute stress-test simulation with custom outbreak parameters")
    public ResponseEntity<ApiResponse<SimulationResultDTO>> runSimulation(@Valid @RequestBody(required = false) SimulationRequestDTO request) {
        if (request == null) {
            request = new SimulationRequestDTO("CUSTOM", null, 50, 100, 14, null);
        }
        SimulationResultDTO result = simulationService.runSimulation(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
