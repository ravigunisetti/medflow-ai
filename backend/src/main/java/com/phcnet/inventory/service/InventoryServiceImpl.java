package com.phcnet.inventory.service;

import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.event.EventPublisher;
import com.phcnet.inventory.dto.InventoryDTO;
import com.phcnet.inventory.dto.InventoryUpdateRequest;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;
    private final EventPublisher eventPublisher;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                PhcRepository phcRepository,
                                MedicineRepository medicineRepository,
                                EventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Page<InventoryDTO> getInventories(Long phcId, Long medicineId, Pageable pageable) {
        return inventoryRepository.findFiltered(phcId, medicineId, pageable).map(InventoryDTO::from);
    }

    @Override
    public List<InventoryDTO> getInventoryByPhc(Long phcId) {
        return inventoryRepository.findByPhcId(phcId).stream().map(InventoryDTO::from).toList();
    }

    @Override
    public InventoryDTO getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .map(InventoryDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record with id " + id + " not found"));
    }

    @Override
    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryUpdateRequest request) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record with id " + id + " not found"));

        inv.setQuantity(request.quantity());
        if (request.reservedQuantity() != null) {
            inv.setReservedQuantity(request.reservedQuantity());
        }
        if (request.batchNumber() != null && !request.batchNumber().isBlank()) {
            inv.setBatchNumber(request.batchNumber());
        }
        if (request.expiryDate() != null) {
            inv.setExpiryDate(request.expiryDate());
        }
        Inventory saved = inventoryRepository.save(inv);
        eventPublisher.publishInventoryUpdated(saved.getPhc().getId(), saved.getMedicine().getId(), saved.getQuantity());
        return InventoryDTO.from(saved);
    }

    @Override
    @Transactional
    public InventoryDTO upsertStock(Long phcId, Long medicineId, Integer quantity) {
        InventoryDTO dto = inventoryRepository.findByPhcIdAndMedicineId(phcId, medicineId)
                .map(inv -> {
                    inv.setQuantity(quantity);
                    return InventoryDTO.from(inventoryRepository.save(inv));
                })
                .orElseGet(() -> {
                    Phc phc = phcRepository.findById(phcId)
                            .orElseThrow(() -> new ResourceNotFoundException("PHC " + phcId + " not found"));
                    Medicine med = medicineRepository.findById(medicineId)
                            .orElseThrow(() -> new ResourceNotFoundException("Medicine " + medicineId + " not found"));
                    Inventory newInv = new Inventory(
                            null,
                            phc,
                            med,
                            quantity,
                            0,
                            "BATCH-MANUAL",
                            LocalDate.now().plusYears(1)
                    );
                    return InventoryDTO.from(inventoryRepository.save(newInv));
                });

        eventPublisher.publishInventoryUpdated(phcId, medicineId, quantity);
        return dto;
    }
}
