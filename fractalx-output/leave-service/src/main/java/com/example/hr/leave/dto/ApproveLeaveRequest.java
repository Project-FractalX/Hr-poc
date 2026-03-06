package com.example.hr.leave.dto;

import jakarta.validation.constraints.*;

public class ApproveLeaveRequest {
    @NotNull  private Long employeeId;
    @NotBlank private String leaveType;     // ANNUAL | SICK | MATERNITY | PATERNITY | UNPAID
    @NotBlank private String startDate;     // ISO date: "2025-07-01"
    @NotBlank private String endDate;
    @NotNull @Min(1) private Integer leaveDays;
    @NotBlank private String reason;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long e) { this.employeeId = e; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String l) { this.leaveType = l; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String s) { this.startDate = s; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String e) { this.endDate = e; }
    public Integer getLeaveDays() { return leaveDays; }
    public void setLeaveDays(Integer l) { this.leaveDays = l; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
}
