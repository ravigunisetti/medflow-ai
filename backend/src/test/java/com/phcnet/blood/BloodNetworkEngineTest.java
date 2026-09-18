package com.phcnet.blood;

import com.phcnet.blood.dto.*;
import com.phcnet.blood.model.*;
import com.phcnet.blood.repository.BloodBankRepository;
import com.phcnet.blood.repository.BloodInventoryRepository;
import com.phcnet.blood.repository.EmergencyBloodRequestRepository;
import com.phcnet.blood.service.BloodCompatibilityService;
import com.phcnet.blood.service.BloodMatchingEngine;
import com.phcnet.blood.service.BloodNetworkService;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BloodNetworkEngineTest {

    @Autowired
    private BloodCompatibilityService compatibilityService;

    @Autowired
    private BloodMatchingEngine matchingEngine;

    @Autowired
    private BloodNetworkService bloodNetworkService;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodInventoryRepository bloodInventoryRepository;

    @Autowired
    private EmergencyBloodRequestRepository requestRepository;

    @Autowired
    private PhcRepository phcRepository;

    @Test
    @DisplayName("ABO and Rh Blood Compatibility Matrix adheres strictly to transfusion science")
    void testTransfusionCompatibilityMatrix() {
        // Universal donor verification: O- can donate to all 8 blood groups
        List<String> allGroups = List.of("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+");
        for (String recipient : allGroups) {
            assertTrue(compatibilityService.isCompatible("O-", recipient),
                    "O- must be compatible with recipient: " + recipient);
        }

        // Universal recipient verification: AB+ can receive from all 8 blood groups
        List<String> abPlusDonors = compatibilityService.getCompatibleDonorGroups("AB+");
        assertEquals(8, abPlusDonors.size(), "AB+ should receive from all 8 donor groups");
        for (String donor : allGroups) {
            assertTrue(compatibilityService.isCompatible(donor, "AB+"),
                    "AB+ must accept donor: " + donor);
        }

        // Rh incompatibility check: Rh+ cannot donate to Rh-
        assertFalse(compatibilityService.isCompatible("O+", "O-"), "O+ cannot donate to O-");
        assertFalse(compatibilityService.isCompatible("A+", "A-"), "A+ cannot donate to A-");
        assertFalse(compatibilityService.isCompatible("B+", "B-"), "B+ cannot donate to B-");
        assertFalse(compatibilityService.isCompatible("AB+", "AB-"), "AB+ cannot donate to AB-");

        // Specific recipient compatibility checks
        List<String> oMinusDonors = compatibilityService.getCompatibleDonorGroups("O-");
        assertEquals(List.of("O-"), oMinusDonors, "O- can only safely receive O-");

        List<String> aMinusDonors = compatibilityService.getCompatibleDonorGroups("A-");
        assertTrue(aMinusDonors.containsAll(List.of("A-", "O-")), "A- can receive A- and O-");
        assertEquals(2, aMinusDonors.size());

        List<String> bPlusDonors = compatibilityService.getCompatibleDonorGroups("B+");
        assertTrue(bPlusDonors.containsAll(List.of("B+", "B-", "O+", "O-")));
        assertEquals(4, bPlusDonors.size());
    }

    @Test
    @DisplayName("Haversine distance and emergency ETA calculation are deterministic")
    void testHaversineDistanceAndEtaCalculation() {
        // Known coordinates: Pune (18.5204, 73.8567) to Satara (17.6805, 73.9936) ~ 94 km
        double dist = matchingEngine.calculateHaversineDistanceKm(18.5204, 73.8567, 17.6805, 73.9936);
        assertTrue(dist > 90.0 && dist < 100.0, "Calculated distance should be ~94 km, got: " + dist);

        // ETA at 40 km/h with 10 min dispatch buffer: (94 / 40) * 60 + 10 ~ 151 mins
        int eta = matchingEngine.calculateEtaMinutes(dist);
        assertTrue(eta >= 140 && eta <= 165, "Expected ETA between 140 and 165 mins, got: " + eta);

        // Zero distance should equal baseline packaging buffer (10 mins)
        double zeroDist = matchingEngine.calculateHaversineDistanceKm(18.5, 73.8, 18.5, 73.8);
        assertEquals(0.0, zeroDist, 0.001);
        assertEquals(10, matchingEngine.calculateEtaMinutes(zeroDist));
    }

    @Test
    @DisplayName("Scoring engine prioritizes verified facilities, closer proximity, and exact blood group matches")
    void testDeterministicMultiFactorScoring() {
        // Verified vs Provisional with same distance
        double scoreVerified = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.VERIFIED, 50, 4, true, true);
        double scoreProvisional = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.PROVISIONAL, 50, 4, true, true);
        assertTrue(scoreVerified > scoreProvisional, "VERIFIED status must yield a higher score than PROVISIONAL");

        // Exact match vs compatible alternative
        double scoreExact = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.VERIFIED, 50, 4, true, true);
        double scoreCompatible = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.VERIFIED, 50, 4, false, true);
        assertTrue(scoreExact > scoreCompatible, "Exact blood match must receive a score bonus");

        // Deadline compliance penalty
        double scoreMet = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.VERIFIED, 50, 4, true, true);
        double scoreMissed = matchingEngine.calculateMatchScore(10.0, 25, VerificationStatus.VERIFIED, 50, 4, true, false);
        assertTrue(scoreMet > scoreMissed, "Missing the critical emergency deadline must penalize the score");
    }

    @Test
    @DisplayName("Matching engine identifies candidates and generates clinical explanation")
    void testMatchingEngineWithActiveRequests() {
        List<EmergencyBloodRequest> requests = requestRepository.findAll();
        EmergencyBloodRequest sampleReq;
        if (requests.isEmpty()) {
            Phc samplePhc = phcRepository.findAll().stream().findFirst().orElseThrow();
            sampleReq = requestRepository.save(new EmergencyBloodRequest(
                    null, samplePhc, "O-", "WHOLE_BLOOD", 2,
                    RequestPriority.CRITICAL, Instant.now().plus(Duration.ofHours(1)),
                    RequestStatus.MATCHING, "Self-contained matching test", null
            ));
        } else {
            sampleReq = requests.get(0);
        }

        BloodMatchingResultDTO matches = bloodNetworkService.findMatchesForRequest(sampleReq.getId());

        assertNotNull(matches);
        assertNotNull(matches.request());
        assertFalse(matches.compatibleGroups().isEmpty());
        assertNotNull(matches.safetyDisclaimer());
        assertTrue(matches.safetyDisclaimer().contains("CLINICAL PROTOCOL NOTICE"));

        if (!matches.candidates().isEmpty()) {
            assertNotNull(matches.recommendedSource());
            assertNotNull(matches.aiExplanation());
            assertTrue(matches.aiExplanation().contains("MEDFLOW DETERMINISTIC RECOMMENDATION"));
            // Recommended source should have the highest match score among deadline-compliant candidates
            CandidateBloodResourceDTO top = matches.candidates().get(0);
            assertEquals(top.bloodBankId(), matches.recommendedSource().bloodBankId());
        }
    }

    @Test
    @DisplayName("Blood transfer lifecycle properly reserves and updates inventory")
    void testTransferLifecycleAndInventoryReservation() {
        List<BloodBank> banks = bloodBankRepository.findByIsActiveTrue();
        assertFalse(banks.isEmpty(), "Active blood banks must exist");
        BloodBank bank = banks.get(0);

        List<Phc> phcs = phcRepository.findAll();
        assertFalse(phcs.isEmpty(), "PHCs must exist");
        Phc phc = phcs.get(0);

        // Check inventory for O+
        List<BloodInventory> invs = bloodInventoryRepository.findByBloodBankId(bank.getId());
        BloodInventory targetInv = invs.stream().filter(i -> "O+".equals(i.getBloodGroup())).findFirst().orElse(null);
        assertNotNull(targetInv, "O+ inventory must exist at bank");
        int initialReserved = targetInv.getReservedUnits();

        // Create an emergency blood request
        CreateBloodRequestDTO reqDTO = new CreateBloodRequestDTO(
                phc.getId(),
                "O+",
                "WHOLE_BLOOD",
                2,
                RequestPriority.HIGH,
                Instant.now().plus(Duration.ofHours(2)),
                "Test emergency transfusion unit reservation"
        );
        EmergencyBloodRequestDTO createdReq = bloodNetworkService.createEmergencyRequest(reqDTO, null);
        assertNotNull(createdReq);
        assertEquals(RequestStatus.MATCHING, createdReq.status());

        // Confirm transfer
        ConfirmTransferRequestDTO confirmDTO = new ConfirmTransferRequestDTO(
                createdReq.id(),
                bank.getId(),
                2,
                15.0,
                32
        );
        BloodTransferDTO transfer = bloodNetworkService.confirmTransfer(confirmDTO);
        assertNotNull(transfer);
        assertEquals("DISPATCH_PENDING", transfer.status());
        assertEquals(2, transfer.units());

        // Verify units are reserved
        BloodInventory updatedInv = bloodInventoryRepository.findById(targetInv.getId()).orElseThrow();
        assertEquals(initialReserved + 2, updatedInv.getReservedUnits(), "Units should be reserved in inventory");

        // Dispatch transfer
        BloodTransferDTO inTransitTransfer = bloodNetworkService.updateTransferStatus(transfer.id(), "IN_TRANSIT");
        assertEquals("IN_TRANSIT", inTransitTransfer.status());
        assertNotNull(inTransitTransfer.dispatchedAt());

        // Complete delivery
        int initialAvailable = updatedInv.getUnitsAvailable();
        BloodTransferDTO deliveredTransfer = bloodNetworkService.updateTransferStatus(transfer.id(), "DELIVERED");
        assertEquals("DELIVERED", deliveredTransfer.status());
        assertNotNull(deliveredTransfer.deliveredAt());

        // Verify units deducted from both available and reserved
        BloodInventory finalInv = bloodInventoryRepository.findById(targetInv.getId()).orElseThrow();
        assertEquals(initialAvailable - 2, finalInv.getUnitsAvailable());
        assertEquals(initialReserved, finalInv.getReservedUnits());
    }

    @Test
    @DisplayName("Emergency Simulation sandbox successfully computes ad-hoc crisis response")
    void testEmergencySimulationSandbox() {
        BloodSimulationRequestDTO simDTO = new BloodSimulationRequestDTO(
                null,
                "O-",
                3,
                RequestPriority.CRITICAL,
                45,
                "MASS_CASUALTY_ACCIDENT"
        );

        BloodMatchingResultDTO result = bloodNetworkService.runEmergencySimulation(simDTO);
        assertNotNull(result);
        assertEquals("O-", result.request().bloodGroup());
        assertEquals(3, result.request().unitsRequired());
        assertNotNull(result.aiExplanation());
        assertNotNull(result.safetyDisclaimer());
    }
}
