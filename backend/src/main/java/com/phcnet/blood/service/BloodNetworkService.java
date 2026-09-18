package com.phcnet.blood.service;

import com.phcnet.blood.dto.*;
import com.phcnet.security.model.User;

import java.util.List;

public interface BloodNetworkService {

    List<BloodBankDTO> getAllBloodBanks(String district);

    BloodBankDTO getBloodBankById(Long id);

    List<BloodInventoryDTO> getBloodBankInventory(Long bloodBankId);

    List<BloodInventoryDTO> getAllBloodInventories();

    List<EmergencyBloodRequestDTO> getAllRequests();

    EmergencyBloodRequestDTO getRequestById(Long id);

    EmergencyBloodRequestDTO createEmergencyRequest(CreateBloodRequestDTO dto, User user);

    BloodMatchingResultDTO findMatchesForRequest(Long requestId);

    BloodTransferDTO confirmTransfer(ConfirmTransferRequestDTO dto);

    BloodTransferDTO updateTransferStatus(Long transferId, String newStatus);

    List<BloodTransferDTO> getAllTransfers();

    BloodDashboardSummaryDTO getDashboardSummary();

    BloodMatchingResultDTO runEmergencySimulation(BloodSimulationRequestDTO simDto);
}
