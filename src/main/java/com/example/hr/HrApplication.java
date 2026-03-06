package com.example.hr;

import org.fractalx.annotations.AdminEnabled;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HR Modular Monolith — FractalX-ready.
 *
 * Modules:
 *   employee    (port 8081) → onboard-employee-saga
 *   payroll     (port 8082) → called by employee & leave sagas
 *   leave       (port 8083) → approve-leave-saga
 *   recruitment (port 8084) → hire-employee-saga
 *   department  (port 8085) → called by employee & recruitment sagas
 *
 * Cross-module calls:
 *   1. EmployeeModule  → PayrollService.setupPayroll()          (onboard saga step 1)
 *   2. EmployeeModule  → DepartmentService.assignToDepartment() (onboard saga step 2)
 *   3. LeaveModule     → EmployeeService.markOnLeave()          (approve-leave saga step 1)
 *   4. LeaveModule     → PayrollService.deductLeave()           (approve-leave saga step 2)
 *   5. RecruitmentModule → EmployeeService.createEmployee()     (hire saga step 1)
 *   6. RecruitmentModule → DepartmentService.incrementHeadcount() (hire saga step 2)
 *
 * Run with:  mvn spring-boot:run
 * Decompose: mvn fractalx:decompose
 */
@SpringBootApplication
@EnableScheduling   // required — outbox poller uses @Scheduled
@AdminEnabled(
    port     = 9090,
    username = "admin",
    password = "admin123"
)
public class HrApplication {
    public static void main(String[] args) {
        SpringApplication.run(HrApplication.class, args);
    }
}
