package com.example.hr.department.dto;

import com.example.hr.department.Department;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
    private Integer headcount;
    private BigDecimal annualBudget;
    private String status;
    private Long managerId;
    private LocalDateTime createdAt;

    public static DepartmentResponse from(Department d) {
        DepartmentResponse r = new DepartmentResponse();
        r.id = d.getId(); r.code = d.getCode(); r.name = d.getName();
        r.headcount = d.getHeadcount(); r.annualBudget = d.getAnnualBudget();
        r.status = d.getStatus(); r.managerId = d.getManagerId();
        r.createdAt = d.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getHeadcount() { return headcount; }
    public BigDecimal getAnnualBudget() { return annualBudget; }
    public String getStatus() { return status; }
    public Long getManagerId() { return managerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
