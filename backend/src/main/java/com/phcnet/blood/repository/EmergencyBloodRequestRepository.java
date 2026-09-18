package com.phcnet.blood.repository;

import com.phcnet.blood.model.EmergencyBloodRequest;
import com.phcnet.blood.model.RequestPriority;
import com.phcnet.blood.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyBloodRequestRepository extends JpaRepository<EmergencyBloodRequest, Long> {
    @Query("SELECT r FROM EmergencyBloodRequest r JOIN FETCH r.hospital ORDER BY r.createdAt DESC")
    List<EmergencyBloodRequest> findAllWithHospital();

    List<EmergencyBloodRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<EmergencyBloodRequest> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
    long countByStatus(RequestStatus status);
    long countByPriority(RequestPriority priority);

    @Query("SELECT r.status, COUNT(r) FROM EmergencyBloodRequest r GROUP BY r.status")
    List<Object[]> countRequestsByStatus();
}
