package com.phcnet.transfer.service;

import com.phcnet.common.exception.BadRequestException;
import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.transfer.dto.TransferCreateRequest;
import com.phcnet.transfer.dto.TransferDTO;
import com.phcnet.transfer.dto.TransferStatusUpdateRequest;
import com.phcnet.transfer.model.Transfer;
import com.phcnet.transfer.repository.TransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final InventoryRepository inventoryRepository;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;

    public TransferServiceImpl(TransferRepository transferRepository,
                               InventoryRepository inventoryRepository,
                               PhcRepository phcRepository,
                               MedicineRepository medicineRepository) {
        this.transferRepository = transferRepository;
        this.inventoryRepository = inventoryRepository;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    public Page<TransferDTO> getTransfers(String status, Pageable pageable) {
        return transferRepository.findByStatus(status, pageable).map(TransferDTO::from);
    }

    @Override
    public List<TransferDTO> getTransfersForPhc(Long phcId) {
        return transferRepository.findByPhcInvolved(phcId).stream().map(TransferDTO::from).toList();
    }

    @Override
    public TransferDTO getTransferById(Long id) {
        return transferRepository.findById(id)
                .map(TransferDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer record " + id + " not found"));
    }

    @Override
    @Transactional
    public TransferDTO createTransfer(TransferCreateRequest request) {
        if (request.sourcePhcId().equals(request.destinationPhcId())) {
            throw new BadRequestException("Source and destination PHC cannot be identical");
        }

        Phc sourcePhc = phcRepository.findById(request.sourcePhcId())
                .orElseThrow(() -> new ResourceNotFoundException("Source PHC " + request.sourcePhcId() + " not found"));
        Phc destPhc = phcRepository.findById(request.destinationPhcId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination PHC " + request.destinationPhcId() + " not found"));
        Medicine medicine = medicineRepository.findById(request.medicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine " + request.medicineId() + " not found"));

        // Validate source inventory availability
        Inventory sourceInv = inventoryRepository.findByPhcIdAndMedicineId(sourcePhc.getId(), medicine.getId())
                .orElseThrow(() -> new BadRequestException("Source PHC does not stock this medicine"));

        int available = sourceInv.getQuantity() - (sourceInv.getReservedQuantity() != null ? sourceInv.getReservedQuantity() : 0);
        if (available < request.quantity()) {
            throw new BadRequestException("Insufficient available stock at source PHC. Available: " + available + ", Requested: " + request.quantity());
        }

        // Calculate Haversine distance if not provided
        double dist = request.distanceKm() != null ? request.distanceKm() :
                calculateHaversineKm(sourcePhc.getLatitude(), sourcePhc.getLongitude(), destPhc.getLatitude(), destPhc.getLongitude());

        // Standard logistics cost model: Rs 200 base + Rs 12/km + Rs 0.05/unit
        double cost = request.estimatedCost() != null ? request.estimatedCost() :
                Math.round((200.0 + (dist * 12.0) + (request.quantity() * 0.05)) * 100.0) / 100.0;

        Transfer transfer = new Transfer(
                null,
                sourcePhc,
                destPhc,
                medicine,
                request.quantity(),
                dist,
                cost,
                "PENDING_APPROVAL",
                request.recommendationReason()
        );

        return TransferDTO.from(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public TransferDTO updateTransferStatus(Long id, TransferStatusUpdateRequest request) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer " + id + " not found"));

        String oldStatus = transfer.getStatus();
        String newStatus = request.status();

        if (oldStatus.equals(newStatus)) {
            return TransferDTO.from(transfer);
        }

        Inventory sourceInv = inventoryRepository.findByPhcIdAndMedicineId(
                transfer.getSourcePhc().getId(), transfer.getMedicine().getId())
                .orElseThrow(() -> new BadRequestException("Source inventory missing"));

        // State transition logic
        if ("APPROVED".equals(newStatus) && "PENDING_APPROVAL".equals(oldStatus)) {
            // Reserve stock at source
            int currentReserved = sourceInv.getReservedQuantity() != null ? sourceInv.getReservedQuantity() : 0;
            sourceInv.setReservedQuantity(currentReserved + transfer.getQuantity());
            inventoryRepository.save(sourceInv);
            transfer.setApprovedByUserId(request.approvedByUserId());
        } else if ("REJECTED".equals(newStatus) && "APPROVED".equals(oldStatus)) {
            // Release reservation
            int currentReserved = sourceInv.getReservedQuantity() != null ? sourceInv.getReservedQuantity() : 0;
            sourceInv.setReservedQuantity(Math.max(0, currentReserved - transfer.getQuantity()));
            inventoryRepository.save(sourceInv);
        } else if ("COMPLETED".equals(newStatus)) {
            // Deduct from source and add to destination
            int currentQty = sourceInv.getQuantity();
            int currentReserved = sourceInv.getReservedQuantity() != null ? sourceInv.getReservedQuantity() : 0;
            sourceInv.setQuantity(Math.max(0, currentQty - transfer.getQuantity()));
            sourceInv.setReservedQuantity(Math.max(0, currentReserved - transfer.getQuantity()));
            inventoryRepository.save(sourceInv);

            // Add to destination
            Inventory destInv = inventoryRepository.findByPhcIdAndMedicineId(
                    transfer.getDestinationPhc().getId(), transfer.getMedicine().getId())
                    .orElseGet(() -> new Inventory(
                            null,
                            transfer.getDestinationPhc(),
                            transfer.getMedicine(),
                            0,
                            0,
                            "BATCH-TRANSFER-" + transfer.getId(),
                            LocalDate.now().plusYears(1)
                    ));

            destInv.setQuantity(destInv.getQuantity() + transfer.getQuantity());
            inventoryRepository.save(destInv);
        }

        transfer.setStatus(newStatus);
        if (request.rejectionReason() != null) {
            transfer.setRecommendationReason(transfer.getRecommendationReason() + " | Rejection reason: " + request.rejectionReason());
        }

        return TransferDTO.from(transferRepository.save(transfer));
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }
}
