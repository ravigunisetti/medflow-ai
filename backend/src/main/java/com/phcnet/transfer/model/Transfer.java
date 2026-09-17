package com.phcnet.transfer.model;

import com.phcnet.medicine.model.Medicine;
import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transfers", indexes = {
    @Index(name = "idx_transfers_status", columnList = "status"),
    @Index(name = "idx_transfers_src_dst", columnList = "source_phc_id, destination_phc_id")
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_phc_id", nullable = false)
    private Phc sourcePhc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_phc_id", nullable = false)
    private Phc destinationPhc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "estimated_cost", nullable = false)
    private Double estimatedCost;

    @Column(nullable = false, length = 30)
    private String status = "PENDING_APPROVAL";

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Transfer() {}

    public Transfer(Long id, Phc sourcePhc, Phc destinationPhc, Medicine medicine, Integer quantity, Double distanceKm, Double estimatedCost, String status, String recommendationReason) {
        this.id = id;
        this.sourcePhc = sourcePhc;
        this.destinationPhc = destinationPhc;
        this.medicine = medicine;
        this.quantity = quantity;
        this.distanceKm = distanceKm;
        this.estimatedCost = estimatedCost;
        this.status = status != null ? status : "PENDING_APPROVAL";
        this.recommendationReason = recommendationReason;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Phc getSourcePhc() { return sourcePhc; }
    public void setSourcePhc(Phc sourcePhc) { this.sourcePhc = sourcePhc; }

    public Phc getDestinationPhc() { return destinationPhc; }
    public void setDestinationPhc(Phc destinationPhc) { this.destinationPhc = destinationPhc; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecommendationReason() { return recommendationReason; }
    public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }

    public Long getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(Long approvedByUserId) { this.approvedByUserId = approvedByUserId; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
