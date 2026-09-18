package com.phcnet.blood.service;

import com.phcnet.blood.dto.*;
import com.phcnet.blood.model.*;
import com.phcnet.blood.repository.BloodBankRepository;
import com.phcnet.blood.repository.BloodInventoryRepository;
import com.phcnet.blood.repository.BloodTransferRepository;
import com.phcnet.blood.repository.EmergencyBloodRequestRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.security.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BloodNetworkServiceImpl implements BloodNetworkService {

    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final EmergencyBloodRequestRepository requestRepository;
    private final BloodTransferRepository transferRepository;
    private final PhcRepository phcRepository;
    private final BloodMatchingEngine matchingEngine;
    private final BloodCompatibilityService compatibilityService;

    public BloodNetworkServiceImpl(BloodBankRepository bloodBankRepository,
                                  BloodInventoryRepository bloodInventoryRepository,
                                  EmergencyBloodRequestRepository requestRepository,
                                  BloodTransferRepository transferRepository,
                                  PhcRepository phcRepository,
                                  BloodMatchingEngine matchingEngine,
                                  BloodCompatibilityService compatibilityService) {
        this.bloodBankRepository = bloodBankRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.requestRepository = requestRepository;
        this.transferRepository = transferRepository;
        this.phcRepository = phcRepository;
        this.matchingEngine = matchingEngine;
        this.compatibilityService = compatibilityService;
    }

    @Override
    public List<BloodBankDTO> getAllBloodBanks(String district) {
        List<BloodBank> banks;
        if (district != null && !district.isBlank()) {
            banks = bloodBankRepository.findByDistrictAndIsActiveTrue(district.trim());
        } else {
            banks = bloodBankRepository.findByIsActiveTrue();
        }
        return banks.stream().map(BloodBankDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public BloodBankDTO getBloodBankById(Long id) {
        return bloodBankRepository.findById(id)
                .map(BloodBankDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Blood Bank not found with id: " + id));
    }

    @Override
    public List<BloodInventoryDTO> getBloodBankInventory(Long bloodBankId) {
        return bloodInventoryRepository.findByBloodBankId(bloodBankId).stream()
                .map(BloodInventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BloodInventoryDTO> getAllBloodInventories() {
        return bloodInventoryRepository.findAll().stream()
                .map(BloodInventoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmergencyBloodRequestDTO> getAllRequests() {
        return requestRepository.findAllWithHospital().stream()
                .map(EmergencyBloodRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencyBloodRequestDTO getRequestById(Long id) {
        return requestRepository.findById(id)
                .map(EmergencyBloodRequestDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Emergency blood request not found with id: " + id));
    }

    @Override
    @Transactional
    public EmergencyBloodRequestDTO createEmergencyRequest(CreateBloodRequestDTO dto, User user) {
        Phc hospital = phcRepository.findById(dto.hospitalId())
                .orElseThrow(() -> new RuntimeException("Hospital/PHC not found with id: " + dto.hospitalId()));

        EmergencyBloodRequest req = new EmergencyBloodRequest(
                null,
                hospital,
                dto.bloodGroup().toUpperCase().trim(),
                dto.componentType() != null ? dto.componentType() : "WHOLE_BLOOD",
                dto.unitsRequired(),
                dto.priority() != null ? dto.priority() : RequestPriority.HIGH,
                dto.requiredBy(),
                RequestStatus.MATCHING,
                dto.clinicalNotes(),
                user
        );

        EmergencyBloodRequest saved = requestRepository.save(req);
        return EmergencyBloodRequestDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public BloodMatchingResultDTO findMatchesForRequest(Long requestId) {
        EmergencyBloodRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Emergency blood request not found with id: " + requestId));

        BloodMatchingResultDTO result = matchingEngine.matchBloodResources(req);
        if (result.recommendedSource() != null && req.getStatus() == RequestStatus.MATCHING) {
            req.setStatus(RequestStatus.MATCH_FOUND);
            requestRepository.save(req);
        }
        return result;
    }

    @Override
    @Transactional
    public BloodTransferDTO confirmTransfer(ConfirmTransferRequestDTO dto) {
        EmergencyBloodRequest req = requestRepository.findById(dto.requestId())
                .orElseThrow(() -> new RuntimeException("Emergency blood request not found with id: " + dto.requestId()));

        BloodBank sourceBank = bloodBankRepository.findById(dto.sourceBloodBankId())
                .orElseThrow(() -> new RuntimeException("Blood bank not found with id: " + dto.sourceBloodBankId()));

        // Find compatible inventory at the bank
        List<String> compatibleGroups = compatibilityService.getCompatibleDonorGroups(req.getBloodGroup());
        List<BloodInventory> bankInventories = bloodInventoryRepository.findByBloodBankId(sourceBank.getId());

        BloodInventory targetInventory = bankInventories.stream()
                .filter(inv -> compatibleGroups.contains(inv.getBloodGroup()))
                .filter(inv -> inv.getUnreservedUnits() >= dto.units())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No compatible inventory with " + dto.units() + " available units found at " + sourceBank.getName()));

        // Reserve units
        targetInventory.setReservedUnits(targetInventory.getReservedUnits() + dto.units());
        targetInventory.setLastUpdated(Instant.now());
        bloodInventoryRepository.save(targetInventory);

        double distance = dto.estimatedDistanceKm() != null ? dto.estimatedDistanceKm() :
                matchingEngine.calculateHaversineDistanceKm(
                        req.getHospital().getLatitude(), req.getHospital().getLongitude(),
                        sourceBank.getLatitude(), sourceBank.getLongitude()
                );
        int eta = dto.estimatedEtaMinutes() != null ? dto.estimatedEtaMinutes() :
                matchingEngine.calculateEtaMinutes(distance);

        BloodTransfer transfer = new BloodTransfer(
                null,
                req,
                sourceBank,
                req.getHospital(),
                dto.units(),
                Math.round(distance * 10.0) / 10.0,
                eta,
                "DISPATCH_PENDING",
                true
        );

        BloodTransfer savedTransfer = transferRepository.save(transfer);

        req.setStatus(RequestStatus.CONFIRMED);
        requestRepository.save(req);

        return BloodTransferDTO.fromEntity(savedTransfer);
    }

    @Override
    @Transactional
    public BloodTransferDTO updateTransferStatus(Long transferId, String newStatus) {
        BloodTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found with id: " + transferId));

        String upperStatus = newStatus.toUpperCase().trim();
        transfer.setStatus(upperStatus);

        EmergencyBloodRequest req = transfer.getRequest();

        if ("IN_TRANSIT".equals(upperStatus)) {
            transfer.setDispatchedAt(Instant.now());
            if (req != null) {
                req.setStatus(RequestStatus.IN_TRANSIT);
            }
        } else if ("DELIVERED".equals(upperStatus) || "FULFILLED".equals(upperStatus)) {
            transfer.setDeliveredAt(Instant.now());
            if (req != null) {
                req.setStatus(RequestStatus.FULFILLED);
            }

            // Deduct from inventory
            List<String> compatibleGroups = compatibilityService.getCompatibleDonorGroups(req != null ? req.getBloodGroup() : "O+");
            List<BloodInventory> bankInventories = bloodInventoryRepository.findByBloodBankId(transfer.getSourceBloodBank().getId());
            bankInventories.stream()
                    .filter(inv -> compatibleGroups.contains(inv.getBloodGroup()))
                    .findFirst()
                    .ifPresent(inv -> {
                        int deductedAvail = Math.max(0, inv.getUnitsAvailable() - transfer.getUnits());
                        int deductedRes = Math.max(0, inv.getReservedUnits() - transfer.getUnits());
                        inv.setUnitsAvailable(deductedAvail);
                        inv.setReservedUnits(deductedRes);
                        inv.setLastUpdated(Instant.now());
                        bloodInventoryRepository.save(inv);
                    });
        } else if ("CANCELLED".equals(upperStatus)) {
            if (req != null) {
                req.setStatus(RequestStatus.CANCELLED);
            }
            // Release reserved units
            List<String> compatibleGroups = compatibilityService.getCompatibleDonorGroups(req != null ? req.getBloodGroup() : "O+");
            List<BloodInventory> bankInventories = bloodInventoryRepository.findByBloodBankId(transfer.getSourceBloodBank().getId());
            bankInventories.stream()
                    .filter(inv -> compatibleGroups.contains(inv.getBloodGroup()))
                    .findFirst()
                    .ifPresent(inv -> {
                        int releasedRes = Math.max(0, inv.getReservedUnits() - transfer.getUnits());
                        inv.setReservedUnits(releasedRes);
                        inv.setLastUpdated(Instant.now());
                        bloodInventoryRepository.save(inv);
                    });
        }

        BloodTransfer updated = transferRepository.save(transfer);
        if (req != null) {
            requestRepository.save(req);
        }

        return BloodTransferDTO.fromEntity(updated);
    }

    @Override
    public List<BloodTransferDTO> getAllTransfers() {
        return transferRepository.findAllWithDetails().stream()
                .map(BloodTransferDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BloodDashboardSummaryDTO getDashboardSummary() {
        long totalBanks = bloodBankRepository.count();
        long activeBanks = bloodBankRepository.countByIsActiveTrue();
        long verifiedBanks = bloodBankRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long totalUnits = Optional.ofNullable(bloodInventoryRepository.sumTotalUnitsAvailable()).orElse(0L);
        long reservedUnits = Optional.ofNullable(bloodInventoryRepository.sumTotalReservedUnits()).orElse(0L);
        long totalReqs = requestRepository.count();
        long activeReqs = requestRepository.countByStatus(RequestStatus.CREATED)
                + requestRepository.countByStatus(RequestStatus.MATCHING)
                + requestRepository.countByStatus(RequestStatus.MATCH_FOUND)
                + requestRepository.countByStatus(RequestStatus.CONFIRMED)
                + requestRepository.countByStatus(RequestStatus.IN_TRANSIT);
        long fulfilledReqs = requestRepository.countByStatus(RequestStatus.FULFILLED);
        long activeTransfers = transferRepository.countByStatus("IN_TRANSIT") + transferRepository.countByStatus("DISPATCH_PENDING");

        Map<String, Long> unitsByGroup = new LinkedHashMap<>();
        List<Object[]> groupCounts = bloodInventoryRepository.sumUnitsGroupedByBloodGroup();
        for (Object[] row : groupCounts) {
            unitsByGroup.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> requestsByStatus = new LinkedHashMap<>();
        List<Object[]> statusCounts = requestRepository.countRequestsByStatus();
        for (Object[] row : statusCounts) {
            requestsByStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        return new BloodDashboardSummaryDTO(
                totalBanks,
                activeBanks,
                verifiedBanks,
                totalUnits,
                reservedUnits,
                totalReqs,
                activeReqs,
                fulfilledReqs,
                activeTransfers,
                unitsByGroup,
                requestsByStatus
        );
    }

    @Override
    public BloodMatchingResultDTO runEmergencySimulation(BloodSimulationRequestDTO simDto) {
        Phc hospital;
        if (simDto.hospitalId() != null) {
            hospital = phcRepository.findById(simDto.hospitalId()).orElseGet(() -> phcRepository.findAll().get(0));
        } else {
            hospital = phcRepository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("No hospitals available for simulation"));
        }

        int deadlineMins = simDto.deadlineMinutes() != null && simDto.deadlineMinutes() > 0 ? simDto.deadlineMinutes() : 60;

        EmergencyBloodRequest simReq = new EmergencyBloodRequest(
                999999L,
                hospital,
                simDto.bloodGroup() != null ? simDto.bloodGroup().toUpperCase().trim() : "O+",
                "WHOLE_BLOOD",
                simDto.unitsRequired() != null ? simDto.unitsRequired() : 4,
                simDto.priority() != null ? simDto.priority() : RequestPriority.CRITICAL,
                Instant.now().plus(Duration.ofMinutes(deadlineMins)),
                RequestStatus.MATCHING,
                "SIMULATION: " + (simDto.scenarioType() != null ? simDto.scenarioType() : "Urgent Regional Emergency Exercise"),
                null
        );

        return matchingEngine.matchBloodResources(simReq);
    }
}
