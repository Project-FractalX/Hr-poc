package com.example.hr.leave.dto;

import com.example.hr.leave.LeaveEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveResponse {
    private Long id;
    private Long employeeId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer leaveDays;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;

    public static LeaveResponse from(LeaveEntry e) {
        LeaveResponse r = new LeaveResponse();
        r.id = e.getId(); r.employeeId = e.getEmployeeId(); r.leaveType = e.getLeaveType();
        r.startDate = e.getStartDate(); r.endDate = e.getEndDate(); r.leaveDays = e.getLeaveDays();
        r.reason = e.getReason(); r.status = e.getStatus(); r.requestedAt = e.getRequestedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getLeaveDays() { return leaveDays; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
}
