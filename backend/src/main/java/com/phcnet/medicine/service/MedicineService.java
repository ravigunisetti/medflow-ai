package com.phcnet.medicine.service;

import com.phcnet.medicine.dto.MedicineCreateRequest;
import com.phcnet.medicine.dto.MedicineDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MedicineService {
    Page<MedicineDTO> getAllMedicines(String category, Pageable pageable);
    List<MedicineDTO> getAllMedicinesList();
    MedicineDTO getMedicineById(Long id);
    MedicineDTO getMedicineByCode(String code);
    MedicineDTO createMedicine(MedicineCreateRequest request);
    void deleteMedicine(Long id);
}
