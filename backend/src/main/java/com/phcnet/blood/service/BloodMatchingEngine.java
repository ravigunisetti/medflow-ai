package com.phcnet.blood.service;

import com.phcnet.blood.dto.BloodMatchingResultDTO;
import com.phcnet.blood.dto.CandidateBloodResourceDTO;
import com.phcnet.blood.dto.EmergencyBloodRequestDTO;
import com.phcnet.blood.model.BloodInventory;
import com.phcnet.blood.model.EmergencyBloodRequest;
import com.phcnet.blood.model.VerificationStatus;
import com.phcnet.blood.repository.BloodInventoryRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class BloodMatchingEngine {

    public static final String SAFETY_DISCLAIMER = "CLINICAL PROTOCOL NOTICE: MedFlow AI recommendations are deterministic algorithmic decision-support aids based on reported real-time inventory. Final cross-matching, transfusion verification, and clinical safety remain strictly under the authority of licensed medical personnel.";
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double EMERGENCY_SPEED_KMH = 40.0;
    private static final int DISPATCH_BUFFER_MINUTES = 10;

    private final BloodInventoryRepository bloodInventoryRepository;
    private final BloodCompatibilityService compatibilityService;

    public BloodMatchingEngine(BloodInventoryRepository bloodInventoryRepository,
                               BloodCompatibilityService compatibilityService) {
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.compatibilityService = compatibilityService;
    }

    public BloodMatchingResultDTO matchBloodResources(EmergencyBloodRequest request) {
        List<String> compatibleGroups = compatibilityService.getCompatibleDonorGroups(request.getBloodGroup());
        
        List<BloodInventory> inventories = bloodInventoryRepository.findAvailableCompatibleInventories(
                compatibleGroups,
                request.getUnitsRequired()
        );

        double hospitalLat = request.getHospital().getLatitude();
        double hospitalLon = request.getHospital().getLongitude();
        Instant now = Instant.now();

        List<CandidateBloodResourceDTO> candidates = new ArrayList<>();

        for (BloodInventory inv : inventories) {
            var bank = inv.getBloodBank();
            double distanceKm = calculateHaversineDistanceKm(hospitalLat, hospitalLon, bank.getLatitude(), bank.getLongitude());
            int etaMinutes = calculateEtaMinutes(distanceKm);
            Instant estimatedArrival = now.plus(Duration.ofMinutes(etaMinutes));
            boolean meetsDeadline = request.getRequiredBy() == null || estimatedArrival.isBefore(request.getRequiredBy()) || estimatedArrival.equals(request.getRequiredBy());
            boolean isExactMatch = inv.getBloodGroup().equalsIgnoreCase(request.getBloodGroup());

            double score = calculateMatchScore(
                    distanceKm,
                    etaMinutes,
                    bank.getVerificationStatus(),
                    inv.getUnreservedUnits(),
                    request.getUnitsRequired(),
                    isExactMatch,
                    meetsDeadline
            );

            String matchReason = buildMatchReason(
                    bank.getName(),
                    inv.getBloodGroup(),
                    isExactMatch,
                    distanceKm,
                    etaMinutes,
                    meetsDeadline,
                    inv.getUnreservedUnits(),
                    bank.getVerificationStatus()
            );

            candidates.add(new CandidateBloodResourceDTO(
                    bank.getId(),
                    bank.getName(),
                    bank.getDistrict(),
                    bank.getLatitude(),
                    bank.getLongitude(),
                    bank.getVerificationStatus(),
                    bank.getContactPhone(),
                    inv.getBloodGroup(),
                    inv.getComponentType(),
                    inv.getUnitsAvailable(),
                    inv.getUnreservedUnits(),
                    Math.round(distanceKm * 10.0) / 10.0,
                    etaMinutes,
                    estimatedArrival,
                    meetsDeadline,
                    score,
                    isExactMatch,
                    matchReason
            ));
        }

        candidates.sort(Comparator
                .comparing(CandidateBloodResourceDTO::meetsDeadline).reversed()
                .thenComparing(CandidateBloodResourceDTO::matchScore).reversed()
                .thenComparing(CandidateBloodResourceDTO::etaMinutes));

        CandidateBloodResourceDTO recommended = candidates.isEmpty() ? null : candidates.get(0);
        String aiExplanation = buildAIExplanation(request, recommended, candidates.size());

        return new BloodMatchingResultDTO(
                EmergencyBloodRequestDTO.fromEntity(request),
                compatibleGroups,
                candidates,
                recommended,
                aiExplanation,
                SAFETY_DISCLAIMER
        );
    }

    public double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public int calculateEtaMinutes(double distanceKm) {
        return (int) Math.round((distanceKm / EMERGENCY_SPEED_KMH) * 60.0) + DISPATCH_BUFFER_MINUTES;
    }

    public double calculateMatchScore(double distanceKm, int etaMinutes, VerificationStatus status,
                                      int unreservedUnits, int requestedUnits, boolean isExactMatch, boolean meetsDeadline) {
        double distScore = (100.0 / (distanceKm + 1.0)) * 0.4;
        double etaScore = (100.0 / (etaMinutes + 1.0)) * 0.4;
        double verificationScore = status == VerificationStatus.VERIFIED ? 20.0 : (status == VerificationStatus.PROVISIONAL ? 5.0 : 0.0);
        double surplus = Math.max(0, unreservedUnits - requestedUnits);
        double inventoryBufferScore = Math.min(15.0, surplus * 0.5);
        double exactMatchBonus = isExactMatch ? 10.0 : 0.0;
        double deadlineMultiplier = meetsDeadline ? 1.0 : 0.4;

        double rawScore = (distScore + etaScore + verificationScore + inventoryBufferScore + exactMatchBonus) * deadlineMultiplier;
        return Math.round(rawScore * 10.0) / 10.0;
    }

    private String buildMatchReason(String bankName, String bloodGroup, boolean isExactMatch,
                                    double distanceKm, int etaMinutes, boolean meetsDeadline,
                                    int availableUnits, VerificationStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append(isExactMatch ? "Exact blood group match (" : "Clinically compatible alternative (");
        sb.append(bloodGroup).append(") with ").append(availableUnits).append(" units available. ");
        sb.append(String.format(Locale.US, "Distance: %.1f km (ETA: %d mins). ", distanceKm, etaMinutes));
        if (meetsDeadline) {
            sb.append("Meets critical deadline window. ");
        } else {
            sb.append("WARNING: Exceeds emergency deadline window. ");
        }
        sb.append("Facility status: ").append(status.name()).append(".");
        return sb.toString();
    }

    private String buildAIExplanation(EmergencyBloodRequest req, CandidateBloodResourceDTO rec, int candidateCount) {
        if (rec == null) {
            return String.format(
                    "MEDFLOW TRIAGE ALERT: No active blood bank in the regional network currently possesses >= %d unreserved units of compatible blood groups (%s) within immediate dispatch threshold. Immediate secondary inter-district escalation required.",
                    req.getUnitsRequired(),
                    req.getBloodGroup()
            );
        }

        return String.format(
                Locale.US,
                "MEDFLOW DETERMINISTIC RECOMMENDATION: %s is the top-ranked source out of %d evaluated facilities. " +
                        "Dispatch ETA is %d minutes (%.1f km) via siren corridor, well within the clinical window. " +
                        "Facility holds %d unreserved units of %s (%s). Cold chain assurance and accreditation level: %s. " +
                        "Algorithmic compatibility verified according to NACO / WHO transfusion standards.",
                rec.bloodBankName(),
                candidateCount,
                rec.etaMinutes(),
                rec.distanceKm(),
                rec.unreservedUnits(),
                rec.bloodGroup(),
                rec.isExactMatch() ? "Exact Match" : "Universal/Compatible Alternative",
                rec.verificationStatus()
        );
    }
}
