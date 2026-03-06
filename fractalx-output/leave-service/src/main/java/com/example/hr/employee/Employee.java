package com.example.hr.employee;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

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
    private String position;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    /**
     * Cross-module reference to department — stored as a plain Long.
     * No @ManyToOne to Department (that entity lives in the department module).
     */
    @Column
    private Long departmentId;

    /**
     * Status is set to "PENDING" when onboarding starts.
     * FractalX's SagaCompletionController will set it to "ACTIVE" (CONFIRMED) or
     * "CANCELLED" depending on whether the saga succeeds or fails.
     */
    @Column(nullable = false)
    private String status;              // PENDING | ACTIVE | ON_LEAVE | TERMINATED | CANCELLED

    @Column(nullable = false)
    private LocalDate hireDate;

    /** Set to true once the payroll has been confirmed by the saga orchestrator. */
    @Column(nullable = false)
    private Boolean payrollSetUp = false;

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
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public Boolean getPayrollSetUp() { return payrollSetUp; }
    public void setPayrollSetUp(Boolean payrollSetUp) { this.payrollSetUp = payrollSetUp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
