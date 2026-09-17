package com.phcnet.transfer.repository;

import com.phcnet.transfer.model.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t JOIN FETCH t.sourcePhc JOIN FETCH t.destinationPhc JOIN FETCH t.medicine WHERE (:status IS NULL OR t.status = :status)")
    Page<Transfer> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.sourcePhc JOIN FETCH t.destinationPhc JOIN FETCH t.medicine WHERE t.sourcePhc.id = :phcId OR t.destinationPhc.id = :phcId")
    List<Transfer> findByPhcInvolved(@Param("phcId") Long phcId);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = 'PENDING_APPROVAL'")
    long countPendingTransfers();

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = 'COMPLETED'")
    long countCompletedTransfers();
}
