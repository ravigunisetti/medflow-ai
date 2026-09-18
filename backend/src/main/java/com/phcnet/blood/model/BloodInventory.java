package com.phcnet.blood.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blood_inventories", indexes = {
        @Index(name = "idx_blood_inv_bank_group", columnList = "blood_bank_id, blood_group"),
        @Index(name = "idx_blood_inv_available", columnList = "blood_group, units_available")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_blood_bank_group_component", columnNames = {"blood_bank_id", "blood_group", "component_type"})
})
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    private BloodBank bloodBank;

    @Column(name = "blood_group", nullable = false, length = 10)
    private String bloodGroup;

    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType = "WHOLE_BLOOD";

    @Column(name = "units_available", nullable = false)
    private Integer unitsAvailable;

    @Column(name = "reserved_units", nullable = false)
    private Integer reservedUnits = 0;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated = Instant.now();

    public BloodInventory() {}

    public BloodInventory(Long id, BloodBank bloodBank, String bloodGroup, String componentType,
                          Integer unitsAvailable, Integer reservedUnits) {
        this.id = id;
        this.bloodBank = bloodBank;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType != null ? componentType : "WHOLE_BLOOD";
        this.unitsAvailable = unitsAvailable;
        this.reservedUnits = reservedUnits != null ? reservedUnits : 0;
        this.lastUpdated = Instant.now();
    }

    public int getUnreservedUnits() {
        return Math.max(0, (unitsAvailable != null ? unitsAvailable : 0) - (reservedUnits != null ? reservedUnits : 0));
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BloodBank getBloodBank() { return bloodBank; }
    public void setBloodBank(BloodBank bloodBank) { this.bloodBank = bloodBank; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public Integer getUnitsAvailable() { return unitsAvailable; }
    public void setUnitsAvailable(Integer unitsAvailable) { this.unitsAvailable = unitsAvailable; }

    public Integer getReservedUnits() { return reservedUnits; }
    public void setReservedUnits(Integer reservedUnits) { this.reservedUnits = reservedUnits; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
