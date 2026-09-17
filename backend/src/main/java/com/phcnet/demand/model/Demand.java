package com.phcnet.demand.model;

import com.phcnet.medicine.model.Medicine;
import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "demands",
    indexes = {
        @Index(name = "idx_demands_lookup", columnList = "phc_id, medicine_id, record_date"),
        @Index(name = "idx_demands_date", columnList = "record_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_phc_med_date", columnNames = {"phc_id", "medicine_id", "record_date"})
    }
)
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phc_id", nullable = false)
    private Phc phc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "quantity_used", nullable = false)
    private Integer quantityUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Demand() {}

    public Demand(Long id, Phc phc, Medicine medicine, LocalDate recordDate, Integer quantityUsed) {
        this.id = id;
        this.phc = phc;
        this.medicine = medicine;
        this.recordDate = recordDate;
        this.quantityUsed = quantityUsed;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Phc getPhc() { return phc; }
    public void setPhc(Phc phc) { this.phc = phc; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public Integer getQuantityUsed() { return quantityUsed; }
    public void setQuantityUsed(Integer quantityUsed) { this.quantityUsed = quantityUsed; }

    public Instant getCreatedAt() { return createdAt; }
}
