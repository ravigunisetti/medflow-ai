package com.phcnet.inventory.service;

import com.phcnet.inventory.dto.InventoryDTO;
import com.phcnet.inventory.dto.InventoryUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    Page<InventoryDTO> getInventories(Long phcId, Long medicineId, Pageable pageable);
    List<InventoryDTO> getInventoryByPhc(Long phcId);
    InventoryDTO getInventoryById(Long id);
    InventoryDTO updateInventory(Long id, InventoryUpdateRequest request);
    InventoryDTO upsertStock(Long phcId, Long medicineId, Integer quantity);
}
