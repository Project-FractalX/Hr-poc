package com.example.hr.employee.dto;

import com.example.hr.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private BigDecimal salary;
    private Long departmentId;
    private String status;
    private LocalDate hireDate;
    private Boolean payrollSetUp;
    private LocalDateTime createdAt;

    public static EmployeeResponse from(Employee e) {
        EmployeeResponse r = new EmployeeResponse();
        r.id = e.getId(); r.firstName = e.getFirstName(); r.lastName = e.getLastName();
        r.email = e.getEmail(); r.position = e.getPosition(); r.salary = e.getSalary();
        r.departmentId = e.getDepartmentId(); r.status = e.getStatus();
        r.hireDate = e.getHireDate(); r.payrollSetUp = e.getPayrollSetUp();
        r.createdAt = e.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPosition() { return position; }
    public BigDecimal getSalary() { return salary; }
    public Long getDepartmentId() { return departmentId; }
    public String getStatus() { return status; }
    public LocalDate getHireDate() { return hireDate; }
    public Boolean getPayrollSetUp() { return payrollSetUp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
