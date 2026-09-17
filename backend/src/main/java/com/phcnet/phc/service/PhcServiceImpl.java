package com.phcnet.phc.service;

import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.phc.dto.PhcCreateRequest;
import com.phcnet.phc.dto.PhcDTO;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PhcServiceImpl implements PhcService {

    private final PhcRepository phcRepository;

    public PhcServiceImpl(PhcRepository phcRepository) {
        this.phcRepository = phcRepository;
    }

    @Override
    public Page<PhcDTO> getAllPhcs(String district, Pageable pageable) {
        if (district != null && !district.isBlank()) {
            return phcRepository.findByDistrictIgnoreCase(district, pageable).map(PhcDTO::from);
        }
        return phcRepository.findAll(pageable).map(PhcDTO::from);
    }

    @Override
    public List<PhcDTO> getAllActivePhcs() {
        return phcRepository.findByIsActiveTrue().stream().map(PhcDTO::from).toList();
    }

    @Override
    public PhcDTO getPhcById(Long id) {
        return phcRepository.findById(id)
                .map(PhcDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("PHC with id " + id + " not found"));
    }

    @Override
    @Transactional
    public PhcDTO createPhc(PhcCreateRequest request) {
        Phc phc = new Phc(
                null,
                request.name(),
                request.district(),
                request.state() != null ? request.state() : "Maharashtra",
                request.latitude(),
                request.longitude(),
                request.populationServed(),
                true
        );
        return PhcDTO.from(phcRepository.save(phc));
    }

    @Override
    @Transactional
    public void deletePhc(Long id) {
        if (!phcRepository.existsById(id)) {
            throw new ResourceNotFoundException("PHC with id " + id + " not found");
        }
        phcRepository.deleteById(id);
    }
}
