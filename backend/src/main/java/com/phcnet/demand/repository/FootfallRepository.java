package com.phcnet.demand.repository;

import com.phcnet.demand.model.PatientFootfall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FootfallRepository extends JpaRepository<PatientFootfall, Long> {

    @Query("SELECT pf FROM PatientFootfall pf JOIN FETCH pf.phc WHERE pf.phc.id = :phcId AND pf.recordDate >= :since ORDER BY pf.recordDate ASC")
    List<PatientFootfall> findHistory(@Param("phcId") Long phcId, @Param("since") LocalDate since);

    @Query("SELECT pf FROM PatientFootfall pf JOIN FETCH pf.phc WHERE (:phcId IS NULL OR pf.phc.id = :phcId)")
    Page<PatientFootfall> findFiltered(@Param("phcId") Long phcId, Pageable pageable);

    @Query("SELECT SUM(pf.patientCount) FROM PatientFootfall pf WHERE pf.recordDate = :recordDate")
    Long getTotalFootfallForDate(@Param("recordDate") LocalDate recordDate);
}
