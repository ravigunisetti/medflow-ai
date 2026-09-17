package com.phcnet.phc.repository;

import com.phcnet.phc.model.Phc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhcRepository extends JpaRepository<Phc, Long> {
    Page<Phc> findByDistrictIgnoreCase(String district, Pageable pageable);
    List<Phc> findByDistrictIgnoreCase(String district);
    List<Phc> findByIsActiveTrue();
}
