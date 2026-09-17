package com.phcnet.simulation.service;

import com.phcnet.simulation.dto.SimulationRequestDTO;
import com.phcnet.simulation.dto.SimulationResultDTO;
import com.phcnet.simulation.dto.SimulationScenarioDTO;

import java.util.List;

public interface SimulationService {
    List<SimulationScenarioDTO> getPredefinedScenarios();
    SimulationResultDTO runSimulation(SimulationRequestDTO request);
}
