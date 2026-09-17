package com.phcnet.prediction.model;

import com.phcnet.medicine.model.Medicine;
import com.phcnet.phc.model.Phc;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "predictions", indexes = {
    @Index(name = "idx_predictions_risk", columnList = "risk_level, created_at"),
    @Index(name = "idx_predictions_phc_med", columnList = "phc_id, medicine_id, created_at")
})
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phc_id", nullable = false)
    private Phc phc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(name = "predicted_daily_demand", nullable = false)
    private Double predictedDailyDemand;

    @Column(name = "predicted_days_to_stockout", nullable = false)
    private Double predictedDaysToStockout;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "baseline-deterministic-v1";

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 1.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Prediction() {}

    public Prediction(Long id, Phc phc, Medicine medicine, Double predictedDailyDemand, Double predictedDaysToStockout, String riskLevel, String modelVersion, Double confidenceScore) {
        this.id = id;
        this.phc = phc;
        this.medicine = medicine;
        this.predictedDailyDemand = predictedDailyDemand;
        this.predictedDaysToStockout = predictedDaysToStockout;
        this.riskLevel = riskLevel;
        this.modelVersion = modelVersion != null ? modelVersion : "baseline-deterministic-v1";
        this.confidenceScore = confidenceScore != null ? confidenceScore : 1.0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Phc getPhc() { return phc; }
    public void setPhc(Phc phc) { this.phc = phc; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public Double getPredictedDailyDemand() { return predictedDailyDemand; }
    public void setPredictedDailyDemand(Double predictedDailyDemand) { this.predictedDailyDemand = predictedDailyDemand; }

    public Double getPredictedDaysToStockout() { return predictedDaysToStockout; }
    public void setPredictedDaysToStockout(Double predictedDaysToStockout) { this.predictedDaysToStockout = predictedDaysToStockout; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Instant getCreatedAt() { return createdAt; }
}
