package com.example.hr.recruitment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String appliedPosition;

    /**
     * Cross-module reference to the target department — stored as a plain Long.
     * No @ManyToOne to Department (different module).
     */
    @Column
    private Long targetDepartmentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal offeredSalary;

    /**
     * Status lifecycle:
     *   APPLIED   → initial state when candidate applies
     *   SHORTLISTED → HR shortlisted the candidate
     *   PENDING   → hire-employee-saga started
     *   HIRED     → saga-complete callback → employee created + dept headcount updated
     *   REJECTED  → application rejected
     *   CANCELLED → saga failed / compensation fired
     */
    @Column(nullable = false)
    private String applicationStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt  = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAppliedPosition() { return appliedPosition; }
    public void setAppliedPosition(String p) { this.appliedPosition = p; }
    public Long getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Long d) { this.targetDepartmentId = d; }
    public BigDecimal getOfferedSalary() { return offeredSalary; }
    public void setOfferedSalary(BigDecimal s) { this.offeredSalary = s; }
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String s) { this.applicationStatus = s; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
