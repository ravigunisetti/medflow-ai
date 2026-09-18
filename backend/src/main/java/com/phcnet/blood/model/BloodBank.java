package com.phcnet.blood.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blood_banks", indexes = {
        @Index(name = "idx_blood_banks_district", columnList = "district"),
        @Index(name = "idx_blood_banks_geo", columnList = "latitude, longitude"),
        @Index(name = "idx_blood_banks_status", columnList = "verification_status, is_active")
})
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String state = "Maharashtra";

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "operating_hours", nullable = false, length = 100)
    private String operatingHours = "24x7 Emergency";

    @Column(name = "storage_capacity_units", nullable = false)
    private Integer storageCapacityUnits = 1000;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BloodBank() {}

    public BloodBank(Long id, String name, String district, String state, Double latitude, Double longitude,
                     VerificationStatus verificationStatus, String contactPhone, String contactEmail,
                     String operatingHours, Integer storageCapacityUnits, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.state = state != null ? state : "Maharashtra";
        this.latitude = latitude;
        this.longitude = longitude;
        this.verificationStatus = verificationStatus != null ? verificationStatus : VerificationStatus.VERIFIED;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.operatingHours = operatingHours != null ? operatingHours : "24x7 Emergency";
        this.storageCapacityUnits = storageCapacityUnits != null ? storageCapacityUnits : 1000;
        this.isActive = isActive != null ? isActive : true;
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public Integer getStorageCapacityUnits() { return storageCapacityUnits; }
    public void setStorageCapacityUnits(Integer storageCapacityUnits) { this.storageCapacityUnits = storageCapacityUnits; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
