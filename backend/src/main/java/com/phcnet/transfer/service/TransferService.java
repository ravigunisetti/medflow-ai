package com.phcnet.transfer.service;

import com.phcnet.transfer.dto.TransferCreateRequest;
import com.phcnet.transfer.dto.TransferDTO;
import com.phcnet.transfer.dto.TransferStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransferService {
    Page<TransferDTO> getTransfers(String status, Pageable pageable);
    List<TransferDTO> getTransfersForPhc(Long phcId);
    TransferDTO getTransferById(Long id);
    TransferDTO createTransfer(TransferCreateRequest request);
    TransferDTO updateTransferStatus(Long id, TransferStatusUpdateRequest request);
}
