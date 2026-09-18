package com.phcnet.blood.repository;

import com.phcnet.blood.model.BloodTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodTransferRepository extends JpaRepository<BloodTransfer, Long> {
    @Query("SELECT t FROM BloodTransfer t JOIN FETCH t.request r JOIN FETCH t.sourceBloodBank sb JOIN FETCH t.destinationHospital dh ORDER BY t.createdAt DESC")
    List<BloodTransfer> findAllWithDetails();

    List<BloodTransfer> findByRequestIdOrderByCreatedAtDesc(Long requestId);
    List<BloodTransfer> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
