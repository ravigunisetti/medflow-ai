package com.phcnet.prediction.service;

import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.demand.service.DemandService;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.prediction.dto.AlertDTO;
import com.phcnet.prediction.dto.PredictionDTO;
import com.phcnet.prediction.dto.RiskExplanationDTO;
import com.phcnet.prediction.model.Prediction;
import com.phcnet.prediction.repository.PredictionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PredictionServiceImpl implements PredictionService {

    private final PredictionRepository predictionRepository;
    private final InventoryRepository inventoryRepository;
    private final DemandService demandService;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;

    @Value("${app.risk-thresholds.critical-days:3.0}")
    private double criticalDays;

    @Value("${app.risk-thresholds.high-days:7.0}")
    private double highDays;

    @Value("${app.risk-thresholds.medium-days:14.0}")
    private double mediumDays;

    public PredictionServiceImpl(PredictionRepository predictionRepository,
                                 InventoryRepository inventoryRepository,
                                 DemandService demandService,
                                 PhcRepository phcRepository,
                                 MedicineRepository medicineRepository) {
        this.predictionRepository = predictionRepository;
        this.inventoryRepository = inventoryRepository;
        this.demandService = demandService;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    public Page<PredictionDTO> getPredictions(String riskLevel, Pageable pageable) {
        return predictionRepository.findByRiskLevel(riskLevel, pageable).map(PredictionDTO::from);
    }

    @Override
    public List<AlertDTO> getActiveAlerts() {
        List<Prediction> alerts = predictionRepository.findCriticalAndHighAlerts();
        List<AlertDTO> result = new ArrayList<>();
        for (Prediction p : alerts) {
            Optional<Inventory> invOpt = inventoryRepository.findByPhcIdAndMedicineId(p.getPhc().getId(), p.getMedicine().getId());
            int stock = invOpt.map(Inventory::getQuantity).orElse(0);
            String msg = String.format("%s at %s has %.1f days of %s remaining (current stock: %d, daily demand: %.1f)",
                    p.getRiskLevel(), p.getPhc().getName(), p.getPredictedDaysToStockout(),
                    p.getMedicine().getName(), stock, p.getPredictedDailyDemand());

            result.add(new AlertDTO(
                    p.getPhc().getId(),
                    p.getPhc().getName(),
                    p.getPhc().getDistrict(),
                    p.getMedicine().getId(),
                    p.getMedicine().getName(),
                    stock,
                    p.getPredictedDailyDemand(),
                    p.getPredictedDaysToStockout(),
                    p.getRiskLevel(),
                    msg,
                    p.getCreatedAt()
            ));
        }
        return result;
    }

    @Override
    @Transactional
    public PredictionDTO calculateAndSaveRisk(Long phcId, Long medicineId) {
        Phc phc = phcRepository.findById(phcId)
                .orElseThrow(() -> new ResourceNotFoundException("PHC " + phcId + " not found"));
        Medicine med = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine " + medicineId + " not found"));

        Optional<Inventory> invOpt = inventoryRepository.findByPhcIdAndMedicineId(phcId, medicineId);
        int currentStock = invOpt.map(Inventory::getQuantity).orElse(0);

        Double avgDaily = demandService.getAverageDailyDemand(phcId, medicineId, 30);
        if (avgDaily == null || avgDaily <= 0.0) {
            avgDaily = Math.max(1.0, med.getSafetyStock() / 30.0);
        }

        double daysRemaining = currentStock / avgDaily;
        String riskLevel = classifyRisk(daysRemaining);

        Prediction prediction = new Prediction(
                null,
                phc,
                med,
                avgDaily,
                daysRemaining,
                riskLevel,
                "deterministic-baseline-v1",
                1.0
        );

        return PredictionDTO.from(predictionRepository.save(prediction));
    }

    @Override
    public RiskExplanationDTO explainRisk(Long phcId, Long medicineId) {
        Phc phc = phcRepository.findById(phcId)
                .orElseThrow(() -> new ResourceNotFoundException("PHC " + phcId + " not found"));
        Medicine med = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine " + medicineId + " not found"));

        Optional<Inventory> invOpt = inventoryRepository.findByPhcIdAndMedicineId(phcId, medicineId);
        int currentStock = invOpt.map(Inventory::getQuantity).orElse(0);

        Double avgDaily = demandService.getAverageDailyDemand(phcId, medicineId, 30);
        if (avgDaily == null || avgDaily <= 0.0) {
            avgDaily = Math.max(1.0, med.getSafetyStock() / 30.0);
        }

        double daysRemaining = Math.round((currentStock / avgDaily) * 10.0) / 10.0;
        String riskLevel = classifyRisk(daysRemaining);

        String formula = "days_to_stockout = current_stock / average_daily_demand";
        String derivation = String.format("%d / %.2f = %.1f days", currentStock, avgDaily, daysRemaining);

        String explanation = String.format(
                "%s currently holds %d units of %s (%s). With a 30-day trailing daily consumption of %.2f units/day, " +
                "available inventory is projected to last %.1f days. Because this is %s the configured threshold of %.1f days, " +
                "it is deterministically classified as %s risk.",
                phc.getName(), currentStock, med.getName(), med.getCode(), avgDaily, daysRemaining,
                daysRemaining < criticalDays ? "below" : "within",
                daysRemaining < criticalDays ? criticalDays : (daysRemaining < highDays ? highDays : mediumDays),
                riskLevel
        );

        return new RiskExplanationDTO(
                phc.getId(),
                phc.getName(),
                phc.getDistrict(),
                med.getId(),
                med.getName(),
                currentStock,
                med.getSafetyStock(),
                Math.round(avgDaily * 100.0) / 100.0,
                daysRemaining,
                riskLevel,
                criticalDays,
                highDays,
                mediumDays,
                formula,
                derivation,
                explanation,
                Instant.now()
        );
    }

    @Override
    @Transactional
    public void evaluateAllFacilities() {
        List<Inventory> allInv = inventoryRepository.findAll();
        for (Inventory inv : allInv) {
            calculateAndSaveRisk(inv.getPhc().getId(), inv.getMedicine().getId());
        }
    }

    private String classifyRisk(double days) {
        if (days < criticalDays) {
            return "CRITICAL";
        } else if (days < highDays) {
            return "HIGH";
        } else if (days <= mediumDays) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
