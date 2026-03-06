package com.example.hr.recruitment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ApplyRequest {
    @NotBlank  private String firstName;
    @NotBlank  private String lastName;
    @NotBlank @Email private String email;
    @NotBlank  private String position;
    @NotNull   private Long departmentId;
    @NotNull @DecimalMin("0") private BigDecimal expectedSalary;

    public String getFirstName() { return firstName; }
    public void setFirstName(String f) { this.firstName = f; }
    public String getLastName() { return lastName; }
    public void setLastName(String l) { this.lastName = l; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getPosition() { return position; }
    public void setPosition(String p) { this.position = p; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long d) { this.departmentId = d; }
    public BigDecimal getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(BigDecimal s) { this.expectedSalary = s; }
}
