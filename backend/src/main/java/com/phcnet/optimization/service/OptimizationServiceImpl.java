package com.phcnet.optimization.service;

import com.phcnet.demand.service.DemandService;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.optimization.dto.OptimizationRequest;
import com.phcnet.optimization.dto.RedistributionRecommendationDTO;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.transfer.dto.TransferCreateRequest;
import com.phcnet.transfer.dto.TransferDTO;
import com.phcnet.transfer.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class OptimizationServiceImpl implements OptimizationService {

    private final InventoryRepository inventoryRepository;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;
    private final DemandService demandService;
    private final TransferService transferService;

    public OptimizationServiceImpl(InventoryRepository inventoryRepository,
                                   PhcRepository phcRepository,
                                   MedicineRepository medicineRepository,
                                   DemandService demandService,
                                   TransferService transferService) {
        this.inventoryRepository = inventoryRepository;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
        this.demandService = demandService;
        this.transferService = transferService;
    }

    @Override
    public List<RedistributionRecommendationDTO> calculateRedistributions(OptimizationRequest request) {
        double maxDist = (request != null && request.maxDistanceKm() != null) ? request.maxDistanceKm() : 75.0;
        double targetDays = (request != null && request.targetStockDays() != null) ? request.targetStockDays() : 14.0;
        Long targetMedId = (request != null) ? request.medicineId() : null;
        String targetDistrict = (request != null) ? request.district() : null;

        List<Inventory> allInventory = inventoryRepository.findAll();
        List<RedistributionRecommendationDTO> recommendations = new ArrayList<>();

        // Group inventory by medicine
        Map<Long, List<Inventory>> byMedicine = new HashMap<>();
        for (Inventory inv : allInventory) {
            if (targetMedId != null && !inv.getMedicine().getId().equals(targetMedId)) {
                continue;
            }
            if (targetDistrict != null && !targetDistrict.isBlank() &&
                    !inv.getPhc().getDistrict().equalsIgnoreCase(targetDistrict)) {
                continue;
            }
            byMedicine.computeIfAbsent(inv.getMedicine().getId(), k -> new ArrayList<>()).add(inv);
        }

        for (Map.Entry<Long, List<Inventory>> entry : byMedicine.entrySet()) {
            List<Inventory> medInvs = entry.getValue();

            // Stratify into Deficit and Candidate Surplus
            List<DeficitNode> deficits = new ArrayList<>();
            List<SurplusNode> surpluses = new ArrayList<>();

            for (Inventory inv : medInvs) {
                Long pId = inv.getPhc().getId();
                Long mId = inv.getMedicine().getId();
                Double avgDaily = demandService.getAverageDailyDemand(pId, mId, 30);
                if (avgDaily == null || avgDaily <= 0.0) {
                    avgDaily = Math.max(1.0, inv.getMedicine().getSafetyStock() / 30.0);
                }

                double daysStock = inv.getQuantity() / avgDaily;

                if (daysStock < 3.5) { // Critical or High deficit
                    int shortage = (int) Math.ceil((targetDays * avgDaily) - inv.getQuantity());
                    if (shortage > 0) {
                        deficits.add(new DeficitNode(inv.getPhc(), inv.getMedicine(), inv.getQuantity(), avgDaily, daysStock, shortage));
                    }
                } else {
                    // Safety buffer: retain safety stock PLUS 10 days of operational demand
                    int mandatoryRetention = (int) Math.ceil(inv.getMedicine().getSafetyStock() + (10.0 * avgDaily));
                    int availableSurplus = inv.getQuantity() - mandatoryRetention;
                    if (availableSurplus > 10) {
                        surpluses.add(new SurplusNode(inv.getPhc(), inv.getMedicine(), inv.getQuantity(), avgDaily, daysStock, availableSurplus));
                    }
                }
            }

            // Greedy min-cost flow / nearest donor matching
            for (DeficitNode def : deficits) {
                if (surpluses.isEmpty()) break;

                // Sort surplus donors by Haversine distance
                surpluses.sort(Comparator.comparingDouble(s -> calculateHaversineKm(
                        s.phc.getLatitude(), s.phc.getLongitude(),
                        def.phc.getLatitude(), def.phc.getLongitude()
                )));

                for (SurplusNode sur : surpluses) {
                    if (sur.availableSurplus <= 0) continue;
                    if (sur.phc.getId().equals(def.phc.getId())) continue;

                    double dist = calculateHaversineKm(
                            sur.phc.getLatitude(), sur.phc.getLongitude(),
                            def.phc.getLatitude(), def.phc.getLongitude()
                    );

                    if (dist > maxDist) continue;

                    int transferQty = Math.min(def.shortageRemaining, sur.availableSurplus);
                    if (transferQty <= 0) continue;

                    // Update state
                    def.shortageRemaining -= transferQty;
                    sur.availableSurplus -= transferQty;

                    double daysBefore = def.daysStock;
                    double daysAfter = Math.round(((def.currentStock + transferQty) / def.avgDaily) * 10.0) / 10.0;
                    double cost = Math.round((200.0 + (dist * 12.0) + (transferQty * 0.05)) * 100.0) / 100.0;

                    String reason = String.format(
                            "Donor %s holds %d surplus units (retains %d units safety buffer). " +
                            "Transferring %d units elevates %s from %.1f days (CRITICAL) to %.1f days (ADEQUATE). Distance: %.1f km.",
                            sur.phc.getName(), sur.initialStock, sur.initialStock - transferQty,
                            transferQty, def.phc.getName(), daysBefore, daysAfter, dist
                    );

                    recommendations.add(new RedistributionRecommendationDTO(
                            sur.phc.getId(),
                            sur.phc.getName(),
                            sur.phc.getDistrict(),
                            def.phc.getId(),
                            def.phc.getName(),
                            def.phc.getDistrict(),
                            def.medicine.getId(),
                            def.medicine.getCode(),
                            def.medicine.getName(),
                            def.medicine.getUnit(),
                            transferQty,
                            dist,
                            cost,
                            "CRITICAL",
                            daysAfter >= 14.0 ? "LOW" : (daysAfter >= 7.0 ? "MEDIUM" : "HIGH"),
                            Math.round(daysBefore * 10.0) / 10.0,
                            daysAfter,
                            reason
                    ));

                    if (def.shortageRemaining <= 0) break;
                }
            }
        }

        return recommendations;
    }

    @Override
    @Transactional
    public List<TransferDTO> applyRecommendations(List<RedistributionRecommendationDTO> recommendations) {
        List<TransferDTO> created = new ArrayList<>();
        for (RedistributionRecommendationDTO rec : recommendations) {
            TransferCreateRequest req = new TransferCreateRequest(
                    rec.sourcePhcId(),
                    rec.destinationPhcId(),
                    rec.medicineId(),
                    rec.quantity(),
                    rec.distanceKm(),
                    rec.estimatedCost(),
                    rec.reason()
            );
            created.add(transferService.createTransfer(req));
        }
        return created;
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }

    private static class DeficitNode {
        Phc phc;
        Medicine medicine;
        int currentStock;
        double avgDaily;
        double daysStock;
        int shortageRemaining;

        DeficitNode(Phc phc, Medicine medicine, int currentStock, double avgDaily, double daysStock, int shortage) {
            this.phc = phc;
            this.medicine = medicine;
            this.currentStock = currentStock;
            this.avgDaily = avgDaily;
            this.daysStock = daysStock;
            this.shortageRemaining = shortage;
        }
    }

    private static class SurplusNode {
        Phc phc;
        Medicine medicine;
        int initialStock;
        double avgDaily;
        double daysStock;
        int availableSurplus;

        SurplusNode(Phc phc, Medicine medicine, int initialStock, double avgDaily, double daysStock, int availableSurplus) {
            this.phc = phc;
            this.medicine = medicine;
            this.initialStock = initialStock;
            this.avgDaily = avgDaily;
            this.daysStock = daysStock;
            this.availableSurplus = availableSurplus;
        }
    }
}
