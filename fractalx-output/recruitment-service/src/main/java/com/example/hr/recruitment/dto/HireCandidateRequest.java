package com.example.hr.recruitment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Request body to start hire-employee-saga. */
public class HireCandidateRequest {
    @NotNull   private Long candidateId;
    @NotBlank  private String firstName;
    @NotBlank  private String lastName;
    @NotBlank @Email private String email;    // official work email (may differ from application)
    @NotBlank  private String position;
    @NotNull @DecimalMin("0") private BigDecimal salary;
    @NotNull   private Long departmentId;

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long c) { this.candidateId = c; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String f) { this.firstName = f; }
    public String getLastName() { return lastName; }
    public void setLastName(String l) { this.lastName = l; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getPosition() { return position; }
    public void setPosition(String p) { this.position = p; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal s) { this.salary = s; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long d) { this.departmentId = d; }
}
