package com.phcnet.medicine.service;

import com.phcnet.common.exception.BadRequestException;
import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.medicine.dto.MedicineCreateRequest;
import com.phcnet.medicine.dto.MedicineDTO;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineServiceImpl(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    @Override
    public Page<MedicineDTO> getAllMedicines(String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return medicineRepository.findByCategoryIgnoreCase(category, pageable).map(MedicineDTO::from);
        }
        return medicineRepository.findAll(pageable).map(MedicineDTO::from);
    }

    @Override
    public List<MedicineDTO> getAllMedicinesList() {
        return medicineRepository.findAll().stream().map(MedicineDTO::from).toList();
    }

    @Override
    public MedicineDTO getMedicineById(Long id) {
        return medicineRepository.findById(id)
                .map(MedicineDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine with id " + id + " not found"));
    }

    @Override
    public MedicineDTO getMedicineByCode(String code) {
        return medicineRepository.findByCode(code)
                .map(MedicineDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine with code " + code + " not found"));
    }

    @Override
    @Transactional
    public MedicineDTO createMedicine(MedicineCreateRequest request) {
        if (medicineRepository.findByCode(request.code()).isPresent()) {
            throw new BadRequestException("Medicine code " + request.code() + " already exists");
        }
        Medicine medicine = new Medicine(
                null,
                request.code(),
                request.name(),
                request.category(),
                request.unit(),
                request.safetyStock(),
                request.shelfLifeDays(),
                request.requiresColdChain()
        );
        return MedicineDTO.from(medicineRepository.save(medicine));
    }

    @Override
    @Transactional
    public void deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medicine with id " + id + " not found");
        }
        medicineRepository.deleteById(id);
    }
}
