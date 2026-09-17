package com.phcnet.medicine.repository;

import com.phcnet.medicine.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByCode(String code);
    Page<Medicine> findByCategoryIgnoreCase(String category, Pageable pageable);
    List<Medicine> findByCategoryIgnoreCase(String category);
}
