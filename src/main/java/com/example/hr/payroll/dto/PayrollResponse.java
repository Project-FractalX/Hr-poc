package com.example.hr.payroll.dto;

import com.example.hr.payroll.PayrollRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollResponse {
    private Long id;
    private Long employeeId;
    private Integer payYear;
    private Integer payMonth;
    private BigDecimal basicSalary;
    private BigDecimal deductions;
    private BigDecimal netSalary;
    private Integer leaveDaysDeducted;
    private String status;
    private LocalDateTime createdAt;

    public static PayrollResponse from(PayrollRecord r) {
        PayrollResponse p = new PayrollResponse();
        p.id = r.getId(); p.employeeId = r.getEmployeeId();
        p.payYear = r.getPayYear(); p.payMonth = r.getPayMonth();
        p.basicSalary = r.getBasicSalary(); p.deductions = r.getDeductions();
        p.netSalary = r.getNetSalary(); p.leaveDaysDeducted = r.getLeaveDaysDeducted();
        p.status = r.getStatus(); p.createdAt = r.getCreatedAt();
        return p;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public Integer getPayYear() { return payYear; }
    public Integer getPayMonth() { return payMonth; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getNetSalary() { return netSalary; }
    public Integer getLeaveDaysDeducted() { return leaveDaysDeducted; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
