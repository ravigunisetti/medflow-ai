package com.phcnet.blood.model;

import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blood_transfers", indexes = {
        @Index(name = "idx_blood_transfer_req", columnList = "request_id"),
        @Index(name = "idx_blood_transfer_status", columnList = "status")
})
public class BloodTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private EmergencyBloodRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_blood_bank_id", nullable = false)
    private BloodBank sourceBloodBank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_hospital_id", nullable = false)
    private Phc destinationHospital;

    @Column(nullable = false)
    private Integer units;

    @Column(name = "estimated_distance_km", nullable = false)
    private Double estimatedDistanceKm;

    @Column(name = "estimated_eta_minutes", nullable = false)
    private Integer estimatedEtaMinutes;

    @Column(nullable = false, length = 30)
    private String status = "DISPATCH_PENDING"; // DISPATCH_PENDING, IN_TRANSIT, DELIVERED, CANCELLED

    @Column(name = "cold_chain_verified", nullable = false)
    private Boolean coldChainVerified = true;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodTransfer() {}

    public BloodTransfer(Long id, EmergencyBloodRequest request, BloodBank sourceBloodBank, Phc destinationHospital,
                         Integer units, Double estimatedDistanceKm, Integer estimatedEtaMinutes, String status,
                         Boolean coldChainVerified) {
        this.id = id;
        this.request = request;
        this.sourceBloodBank = sourceBloodBank;
        this.destinationHospital = destinationHospital;
        this.units = units;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedEtaMinutes = estimatedEtaMinutes;
        this.status = status != null ? status : "DISPATCH_PENDING";
        this.coldChainVerified = coldChainVerified != null ? coldChainVerified : true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmergencyBloodRequest getRequest() { return request; }
    public void setRequest(EmergencyBloodRequest request) { this.request = request; }

    public BloodBank getSourceBloodBank() { return sourceBloodBank; }
    public void setSourceBloodBank(BloodBank sourceBloodBank) { this.sourceBloodBank = sourceBloodBank; }

    public Phc getDestinationHospital() { return destinationHospital; }
    public void setDestinationHospital(Phc destinationHospital) { this.destinationHospital = destinationHospital; }

    public Integer getUnits() { return units; }
    public void setUnits(Integer units) { this.units = units; }

    public Double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public void setEstimatedDistanceKm(Double estimatedDistanceKm) { this.estimatedDistanceKm = estimatedDistanceKm; }

    public Integer getEstimatedEtaMinutes() { return estimatedEtaMinutes; }
    public void setEstimatedEtaMinutes(Integer estimatedEtaMinutes) { this.estimatedEtaMinutes = estimatedEtaMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getColdChainVerified() { return coldChainVerified; }
    public void setColdChainVerified(Boolean coldChainVerified) { this.coldChainVerified = coldChainVerified; }

    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
