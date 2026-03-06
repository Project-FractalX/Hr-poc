package com.example.hr.payroll;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_records")
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the employee — cross-module reference stored as a plain ID.
     * No @ManyToOne to Employee: that would cross the employee module boundary.
     */
    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Integer payYear;

    @Column(nullable = false)
    private Integer payMonth;       // 1-12

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    /** Number of approved leave days deducted this month. */
    @Column(nullable = false)
    private Integer leaveDaysDeducted = 0;

    @Column(nullable = false)
    private String status;          // "ACTIVE", "PROCESSED", "CANCELLED"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (deductions == null) deductions = BigDecimal.ZERO;
        if (netSalary == null) netSalary = basicSalary;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getPayYear() { return payYear; }
    public void setPayYear(Integer payYear) { this.payYear = payYear; }
    public Integer getPayMonth() { return payMonth; }
    public void setPayMonth(Integer payMonth) { this.payMonth = payMonth; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public Integer getLeaveDaysDeducted() { return leaveDaysDeducted; }
    public void setLeaveDaysDeducted(Integer leaveDaysDeducted) { this.leaveDaysDeducted = leaveDaysDeducted; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
