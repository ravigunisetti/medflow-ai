package com.phcnet.blood.repository;

import com.phcnet.blood.model.BloodBank;
import com.phcnet.blood.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {
    List<BloodBank> findByIsActiveTrue();
    List<BloodBank> findByDistrictAndIsActiveTrue(String district);
    long countByVerificationStatus(VerificationStatus status);
    long countByIsActiveTrue();
}
