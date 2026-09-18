package com.phcnet.blood.model;

import com.phcnet.phc.model.Phc;
import com.phcnet.security.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "emergency_blood_requests", indexes = {
        @Index(name = "idx_blood_req_status", columnList = "status, priority"),
        @Index(name = "idx_blood_req_hospital", columnList = "hospital_id, created_at"),
        @Index(name = "idx_blood_req_deadline", columnList = "required_by")
})
public class EmergencyBloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Phc hospital;

    @Column(name = "blood_group", nullable = false, length = 10)
    private String bloodGroup;

    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType = "WHOLE_BLOOD";

    @Column(name = "units_required", nullable = false)
    private Integer unitsRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority = RequestPriority.HIGH;

    @Column(name = "required_by", nullable = false)
    private Instant requiredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.CREATED;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EmergencyBloodRequest() {}

    public EmergencyBloodRequest(Long id, Phc hospital, String bloodGroup, String componentType,
                                 Integer unitsRequired, RequestPriority priority, Instant requiredBy,
                                 RequestStatus status, String clinicalNotes, User createdByUser) {
        this.id = id;
        this.hospital = hospital;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType != null ? componentType : "WHOLE_BLOOD";
        this.unitsRequired = unitsRequired;
        this.priority = priority != null ? priority : RequestPriority.HIGH;
        this.requiredBy = requiredBy;
        this.status = status != null ? status : RequestStatus.CREATED;
        this.clinicalNotes = clinicalNotes;
        this.createdByUser = createdByUser;
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

    public Phc getHospital() { return hospital; }
    public void setHospital(Phc hospital) { this.hospital = hospital; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public Integer getUnitsRequired() { return unitsRequired; }
    public void setUnitsRequired(Integer unitsRequired) { this.unitsRequired = unitsRequired; }

    public RequestPriority getPriority() { return priority; }
    public void setPriority(RequestPriority priority) { this.priority = priority; }

    public Instant getRequiredBy() { return requiredBy; }
    public void setRequiredBy(Instant requiredBy) { this.requiredBy = requiredBy; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public User getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
