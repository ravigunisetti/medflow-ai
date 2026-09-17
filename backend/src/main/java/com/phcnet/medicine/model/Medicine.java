package com.phcnet.medicine.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicines_category", columnList = "category")
})
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "shelf_life_days", nullable = false)
    private Integer shelfLifeDays = 730;

    @Column(name = "requires_cold_chain", nullable = false)
    private Boolean requiresColdChain = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Medicine() {}

    public Medicine(Long id, String code, String name, String category, String unit, Integer safetyStock, Integer shelfLifeDays, Boolean requiresColdChain) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.safetyStock = safetyStock;
        this.shelfLifeDays = shelfLifeDays != null ? shelfLifeDays : 730;
        this.requiresColdChain = requiresColdChain != null ? requiresColdChain : false;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getSafetyStock() { return safetyStock; }
    public void setSafetyStock(Integer safetyStock) { this.safetyStock = safetyStock; }

    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }

    public Boolean getRequiresColdChain() { return requiresColdChain; }
    public void setRequiresColdChain(Boolean requiresColdChain) { this.requiresColdChain = requiresColdChain; }

    public Instant getCreatedAt() { return createdAt; }
}
