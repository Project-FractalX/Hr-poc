package com.example.hr.department.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateDepartmentRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    @NotNull @DecimalMin("0") private BigDecimal annualBudget;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(BigDecimal annualBudget) { this.annualBudget = annualBudget; }
}
