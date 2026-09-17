package com.phcnet.phc.service;

import com.phcnet.phc.dto.PhcCreateRequest;
import com.phcnet.phc.dto.PhcDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PhcService {
    Page<PhcDTO> getAllPhcs(String district, Pageable pageable);
    List<PhcDTO> getAllActivePhcs();
    PhcDTO getPhcById(Long id);
    PhcDTO createPhc(PhcCreateRequest request);
    void deletePhc(Long id);
}
