package com.phcnet.demand.model;

import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patient_footfalls",
    indexes = {
        @Index(name = "idx_footfall_phc_date", columnList = "phc_id, record_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_phc_footfall_date", columnNames = {"phc_id", "record_date"})
    }
)
public class PatientFootfall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phc_id", nullable = false)
    private Phc phc;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "patient_count", nullable = false)
    private Integer patientCount;

    @Column(name = "emergency_count", nullable = false)
    private Integer emergencyCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PatientFootfall() {}

    public PatientFootfall(Long id, Phc phc, LocalDate recordDate, Integer patientCount, Integer emergencyCount) {
        this.id = id;
        this.phc = phc;
        this.recordDate = recordDate;
        this.patientCount = patientCount;
        this.emergencyCount = emergencyCount != null ? emergencyCount : 0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Phc getPhc() { return phc; }
    public void setPhc(Phc phc) { this.phc = phc; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public Integer getPatientCount() { return patientCount; }
    public void setPatientCount(Integer patientCount) { this.patientCount = patientCount; }

    public Integer getEmergencyCount() { return emergencyCount; }
    public void setEmergencyCount(Integer emergencyCount) { this.emergencyCount = emergencyCount; }

    public Instant getCreatedAt() { return createdAt; }
}
