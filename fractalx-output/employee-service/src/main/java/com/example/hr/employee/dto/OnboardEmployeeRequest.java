package com.example.hr.employee.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class OnboardEmployeeRequest {
    @NotBlank  private String firstName;
    @NotBlank  private String lastName;
    @NotBlank @Email private String email;
    @NotBlank  private String position;
    @NotNull @DecimalMin("0") private BigDecimal salary;
    @NotNull   private Long departmentId;

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
