package com.example.hr.recruitment.dto;

import com.example.hr.recruitment.Candidate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CandidateResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String appliedPosition;
    private Long targetDepartmentId;
    private BigDecimal offeredSalary;
    private String applicationStatus;
    private LocalDateTime appliedAt;

    public static CandidateResponse from(Candidate c) {
        CandidateResponse r = new CandidateResponse();
        r.id = c.getId(); r.firstName = c.getFirstName(); r.lastName = c.getLastName();
        r.email = c.getEmail(); r.appliedPosition = c.getAppliedPosition();
        r.targetDepartmentId = c.getTargetDepartmentId();
        r.offeredSalary = c.getOfferedSalary(); r.applicationStatus = c.getApplicationStatus();
        r.appliedAt = c.getAppliedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getAppliedPosition() { return appliedPosition; }
    public Long getTargetDepartmentId() { return targetDepartmentId; }
    public BigDecimal getOfferedSalary() { return offeredSalary; }
    public String getApplicationStatus() { return applicationStatus; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
}
