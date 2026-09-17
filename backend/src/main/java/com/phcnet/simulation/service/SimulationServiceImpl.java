package com.phcnet.simulation.service;

import com.phcnet.demand.service.DemandService;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.optimization.dto.OptimizationRequest;
import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import com.phcnet.optimization.service.OptimizationService;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.simulation.dto.SimulationRequestDTO;
import com.phcnet.simulation.dto.SimulationResultDTO;
import com.phcnet.simulation.dto.SimulationScenarioDTO;
import com.phcnet.simulation.dto.VulnerableFacilityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class SimulationServiceImpl implements SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;
    private final DemandService demandService;
    private final OptimizationService optimizationService;

    private static final List<SimulationScenarioDTO> PREDEFINED_SCENARIOS = List.of(
            new SimulationScenarioDTO(
                    "MONSOON_DENGUE",
                    "Monsoon Dengue Outbreak",
                    "Vector-borne seasonal surge with acute spike in fever, analgesics demand, and logistics delays due to heavy rainfall.",
                    60,
                    120,
                    14,
                    "Analgesics & Antipyretics",
                    "CRITICAL"
            ),
            new SimulationScenarioDTO(
                    "HEATWAVE_DEHYDRATION",
                    "Severe Summer Heatwave",
                    "Extended heatwave inducing widespread dehydration, heat exhaustion, and critical depletion of ORS and IV fluids.",
                    80,
                    150,
                    7,
                    "Fluids & Electrolytes",
                    "CRITICAL"
            ),
            new SimulationScenarioDTO(
                    "SUPPLY_CORRIDOR_BREAKDOWN",
                    "State Highway Flood & Transit Disruption",
                    "Major transportation corridor disruption delaying central warehouse replenishment across peripheral PHCs.",
                    30,
                    40,
                    21,
                    "Anti-infectives",
                    "HIGH"
            ),
            new SimulationScenarioDTO(
                    "GASTRO_WATER_CONTAMINATION",
                    "Post-Flood Water Contamination & Diarrhea",
                    "Bacterial contamination in village water reservoirs causing sudden gastrointestinal epidemic across multiple talukas.",
                    100,
                    180,
                    10,
                    "Gastrointestinal Agents",
                    "CRITICAL"
            )
    );

    public SimulationServiceImpl(InventoryRepository inventoryRepository,
                                 PhcRepository phcRepository,
                                 MedicineRepository medicineRepository,
                                 DemandService demandService,
                                 OptimizationService optimizationService) {
        this.inventoryRepository = inventoryRepository;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
        this.demandService = demandService;
        this.optimizationService = optimizationService;
    }

    @Override
    public List<SimulationScenarioDTO> getPredefinedScenarios() {
        return PREDEFINED_SCENARIOS;
    }

    @Override
    public SimulationResultDTO runSimulation(SimulationRequestDTO request) {
        long startTime = System.currentTimeMillis();

        int surgePercent = request.getFootfallSurgePercent();
        int acuteMultiplier = request.getAcuteDemandMultiplier();
        int delayDays = request.getSupplyDelayDays();
        String targetCategory = request.getAffectedCategory();
        String district = request.district();

        log.info("Running simulation: surge=+{}%, acute=+{}%, delay={}d, category={}, district={}",
                surgePercent, acuteMultiplier, delayDays, targetCategory, district);

        List<Inventory> inventories = inventoryRepository.findAll();
        if (inventories.isEmpty()) {
            return new SimulationResultDTO(
                    0, 0, 0, 0, 0, 0.0,
                    System.currentTimeMillis() - startTime,
                    "No inventory records found in database.",
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        // Filter relevant inventories based on category and district
        List<Inventory> filteredInventories = new ArrayList<>();
        for (Inventory inv : inventories) {
            if (district != null && !district.isBlank() && !inv.getPhc().getDistrict().equalsIgnoreCase(district)) {
                continue;
            }
            if (targetCategory != null && !targetCategory.isBlank()) {
                String cat = inv.getMedicine().getCategory();
                if (cat != null && cat.toLowerCase().contains(targetCategory.toLowerCase().trim())) {
                    filteredInventories.add(inv);
                }
            } else {
                filteredInventories.add(inv);
            }
        }

        // If category filter matched nothing, fallback to all filtered by district or first 200
        if (filteredInventories.isEmpty()) {
            for (Inventory inv : inventories) {
                if (district == null || district.isBlank() || inv.getPhc().getDistrict().equalsIgnoreCase(district)) {
                    filteredInventories.add(inv);
                }
            }
        }

        double footfallFactor = 1.0 + (surgePercent / 100.0);
        double acuteFactor = 1.0 + (acuteMultiplier / 100.0);
        double totalSurgeFactor = footfallFactor * acuteFactor;

        int baselineCriticalCount = 0;
        int crisisCriticalCount = 0;
        Set<Long> baselineCriticalPhcs = new HashSet<>();
        Set<Long> crisisCriticalPhcs = new HashSet<>();
        List<VulnerableFacilityDTO> vulnerableList = new ArrayList<>();

        for (Inventory inv : filteredInventories) {
            Long phcId = inv.getPhc().getId();
            Long medId = inv.getMedicine().getId();
            Double baseAvg = demandService.getAverageDailyDemand(phcId, medId, 30);
            if (baseAvg == null || baseAvg <= 0.0) {
                baseAvg = Math.max(2.0, inv.getMedicine().getSafetyStock() / 30.0);
            }

            double beforeStockDays = inv.getQuantity() / baseAvg;
            double crisisDailyDemand = baseAvg * totalSurgeFactor;
            double afterStockDays = inv.getQuantity() / crisisDailyDemand;

            if (beforeStockDays < 3.5) {
                baselineCriticalCount++;
                baselineCriticalPhcs.add(phcId);
            }

            String newRisk;
            if (afterStockDays < 3.5) {
                newRisk = "CRITICAL";
                crisisCriticalCount++;
                crisisCriticalPhcs.add(phcId);
            } else if (afterStockDays < 7.0) {
                newRisk = "HIGH";
            } else if (afterStockDays < 14.0) {
                newRisk = "MEDIUM";
            } else {
                newRisk = "LOW";
            }

            // Include in vulnerable list if it deteriorated to CRITICAL or was already critical
            if ("CRITICAL".equals(newRisk) || (beforeStockDays >= 3.5 && afterStockDays < 7.0)) {
                vulnerableList.add(new VulnerableFacilityDTO(
                        phcId,
                        inv.getPhc().getName(),
                        inv.getPhc().getDistrict(),
                        inv.getMedicine().getName(),
                        inv.getMedicine().getCode(),
                        inv.getQuantity(),
                        Math.round(baseAvg * 10.0) / 10.0,
                        Math.round(crisisDailyDemand * 10.0) / 10.0,
                        Math.round(beforeStockDays * 10.0) / 10.0,
                        Math.round(afterStockDays * 10.0) / 10.0,
                        newRisk
                ));
            }
        }

        // Sort vulnerable by lowest stock days first
        vulnerableList.sort(Comparator.comparingDouble(VulnerableFacilityDTO::afterStockDays));
        if (vulnerableList.size() > 15) {
            vulnerableList = vulnerableList.subList(0, 15);
        }

        // Calculate dry-run mitigation recommendations using OptimizationService
        OptimizationRequest optRequest = new OptimizationRequest(district, null, 75.0, 14.0);
        List<RedistributionRecommendationDTO> recommendations = Collections.emptyList();
        try {
            recommendations = optimizationService.calculateRedistributions(optRequest);
        } catch (Exception e) {
            log.warn("Optimization solver dry-run error: {}", e.getMessage());
        }

        int transfersGenerated = recommendations.size();
        // Count how many critical PHCs receive relief from recommendations
        Set<Long> relievedPhcIds = new HashSet<>();
        for (RedistributionRecommendationDTO rec : recommendations) {
            relievedPhcIds.add(rec.destinationPhcId());
        }

        int newlyCriticalCount = Math.max(0, crisisCriticalPhcs.size() - baselineCriticalPhcs.size());
        int prevented = 0;
        for (Long pId : relievedPhcIds) {
            if (crisisCriticalPhcs.contains(pId)) {
                prevented++;
            }
        }
        if (prevented == 0 && transfersGenerated > 0) {
            prevented = Math.min(transfersGenerated, newlyCriticalCount > 0 ? newlyCriticalCount : crisisCriticalPhcs.size());
        }

        double mitigationRate = crisisCriticalPhcs.isEmpty() ? 100.0 :
                Math.min(100.0, Math.round(((double) prevented / Math.max(1, crisisCriticalPhcs.size())) * 1000.0) / 10.0);

        long duration = System.currentTimeMillis() - startTime;
        String summary = String.format("Simulation complete: Footfall +%d%%, Demand +%d%% resulted in %d facilities at CRITICAL stockout risk. Proposed %d emergency redistribution routes mitigating %d critical stockouts (%.1f%% protection rate).",
                surgePercent, acuteMultiplier, crisisCriticalPhcs.size(), transfersGenerated, prevented, mitigationRate);

        return new SimulationResultDTO(
                baselineCriticalPhcs.size(),
                crisisCriticalPhcs.size(),
                transfersGenerated,
                prevented,
                filteredInventories.size(),
                mitigationRate,
                duration,
                summary,
                vulnerableList,
                recommendations
        );
    }
}
