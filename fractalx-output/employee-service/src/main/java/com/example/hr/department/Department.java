package com.example.hr.department;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;                    // e.g. "ENG", "HR", "FIN"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer headcount = 0;          // incremented when employees are hired

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal annualBudget;

    @Column(nullable = false)
    private String status;                  // "ACTIVE", "INACTIVE"

    /**
     * ID of the department manager — stored as a plain Long (cross-module reference).
     * No @ManyToOne to Employee: that would cross the employee module boundary.
     */
    @Column
    private Long managerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getHeadcount() { return headcount; }
    public void setHeadcount(Integer headcount) { this.headcount = headcount; }

    public BigDecimal getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(BigDecimal annualBudget) { this.annualBudget = annualBudget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
