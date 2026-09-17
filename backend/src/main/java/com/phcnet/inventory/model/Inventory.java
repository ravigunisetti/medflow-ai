package com.phcnet.inventory.model;

import com.phcnet.medicine.model.Medicine;
import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventories",
    indexes = {
        @Index(name = "idx_inventories_phc_med", columnList = "phc_id, medicine_id"),
        @Index(name = "idx_inventories_quantity", columnList = "quantity")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_phc_medicine_batch", columnNames = {"phc_id", "medicine_id", "batch_number"})
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phc_id", nullable = false)
    private Phc phc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber = "BATCH-DEFAULT";

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Inventory() {}

    public Inventory(Long id, Phc phc, Medicine medicine, Integer quantity, Integer reservedQuantity, String batchNumber, LocalDate expiryDate) {
        this.id = id;
        this.phc = phc;
        this.medicine = medicine;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity != null ? reservedQuantity : 0;
        this.batchNumber = batchNumber != null ? batchNumber : "BATCH-DEFAULT";
        this.expiryDate = expiryDate;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Phc getPhc() { return phc; }
    public void setPhc(Phc phc) { this.phc = phc; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Instant getUpdatedAt() { return updatedAt; }
}
