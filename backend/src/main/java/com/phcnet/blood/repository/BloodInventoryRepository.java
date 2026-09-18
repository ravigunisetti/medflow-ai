package com.phcnet.blood.repository;

import com.phcnet.blood.model.BloodBank;
import com.phcnet.blood.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {
    List<BloodInventory> findByBloodBankId(Long bloodBankId);
    List<BloodInventory> findByBloodBankIn(List<BloodBank> bloodBanks);
    Optional<BloodInventory> findByBloodBankIdAndBloodGroupAndComponentType(Long bloodBankId, String bloodGroup, String componentType);

    @Query("SELECT b FROM BloodInventory b JOIN FETCH b.bloodBank bb WHERE b.bloodGroup IN :groups AND bb.isActive = true AND (b.unitsAvailable - b.reservedUnits) >= :minUnits")
    List<BloodInventory> findAvailableCompatibleInventories(@Param("groups") List<String> groups, @Param("minUnits") Integer minUnits);

    @Query("SELECT COALESCE(SUM(b.unitsAvailable), 0) FROM BloodInventory b")
    Long sumTotalUnitsAvailable();

    @Query("SELECT COALESCE(SUM(b.reservedUnits), 0) FROM BloodInventory b")
    Long sumTotalReservedUnits();

    @Query("SELECT b.bloodGroup, COALESCE(SUM(b.unitsAvailable), 0) FROM BloodInventory b GROUP BY b.bloodGroup")
    List<Object[]> sumUnitsGroupedByBloodGroup();
}
