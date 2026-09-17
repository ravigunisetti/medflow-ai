package com.phcnet.dashboard.service;

import com.phcnet.dashboard.dto.DashboardSummaryDTO;
import com.phcnet.dashboard.dto.DistrictRiskSummaryDTO;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.prediction.model.Prediction;
import com.phcnet.prediction.repository.PredictionRepository;
import com.phcnet.transfer.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final PredictionRepository predictionRepository;
    private final TransferRepository transferRepository;

    public DashboardServiceImpl(PhcRepository phcRepository,
                                MedicineRepository medicineRepository,
                                InventoryRepository inventoryRepository,
                                PredictionRepository predictionRepository,
                                TransferRepository transferRepository) {
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.predictionRepository = predictionRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    public DashboardSummaryDTO getSummary() {
        long totalPhcs = phcRepository.count();
        long totalMeds = medicineRepository.count();
        long criticalStockouts = inventoryRepository.countCriticalStockouts();
        long highRiskPreds = predictionRepository.countHighPredictions();
        long pendingTransfers = transferRepository.countPendingTransfers();
        long completedTransfers = transferRepository.countCompletedTransfers();

        // Calculate Network Health Score (0 - 100)
        double healthScore = 100.0;
        if (totalPhcs > 0 && totalMeds > 0) {
            double stockoutRatio = (double) criticalStockouts / (totalPhcs * Math.max(1, totalMeds));
            healthScore = Math.max(0.0, Math.round((1.0 - (stockoutRatio * 2.5)) * 1000.0) / 10.0);
        }

        // District Breakdown
        List<Phc> allPhcs = phcRepository.findAll();
        Map<String, List<Phc>> phcsByDistrict = allPhcs.stream()
                .collect(Collectors.groupingBy(Phc::getDistrict));

        List<Prediction> allPredictions = predictionRepository.findAll();
        Map<Long, List<Prediction>> predsByPhc = allPredictions.stream()
                .collect(Collectors.groupingBy(p -> p.getPhc().getId()));

        List<DistrictRiskSummaryDTO> districtSummaries = new ArrayList<>();
        for (Map.Entry<String, List<Phc>> entry : phcsByDistrict.entrySet()) {
            String district = entry.getKey();
            List<Phc> dPhcs = entry.getValue();
            long critCount = 0;
            long highCount = 0;
            double sumDays = 0.0;
            long daysCount = 0;

            for (Phc p : dPhcs) {
                List<Prediction> preds = predsByPhc.getOrDefault(p.getId(), Collections.emptyList());
                boolean hasCrit = preds.stream().anyMatch(pr -> "CRITICAL".equals(pr.getRiskLevel()));
                boolean hasHigh = preds.stream().anyMatch(pr -> "HIGH".equals(pr.getRiskLevel()));
                if (hasCrit) critCount++;
                else if (hasHigh) highCount++;

                for (Prediction pr : preds) {
                    sumDays += pr.getPredictedDaysToStockout();
                    daysCount++;
                }
            }

            double avgDays = daysCount > 0 ? Math.round((sumDays / daysCount) * 10.0) / 10.0 : 15.0;
            String overallStatus = critCount > 0 ? "CRITICAL" : (highCount > 0 ? "VULNERABLE" : "STABLE");

            districtSummaries.add(new DistrictRiskSummaryDTO(
                    district,
                    dPhcs.size(),
                    critCount,
                    highCount,
                    avgDays,
                    overallStatus
            ));
        }

        districtSummaries.sort(Comparator.comparing(DistrictRiskSummaryDTO::district));

        return new DashboardSummaryDTO(
                totalPhcs,
                totalMeds,
                criticalStockouts,
                highRiskPreds,
                pendingTransfers,
                completedTransfers,
                healthScore,
                districtSummaries
        );
    }
}
