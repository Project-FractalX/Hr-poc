package com.example.hr.employee;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.fractalx.generated.employeeservice.outbox.OutboxPublisher;

/**
 * Employee module boundary.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SAGA: onboard-employee-saga                                             │
 * │                                                                           │
 * │  1. Save Employee(status=PENDING) locally                                │
 * │  2. payrollService.setupPayroll(employeeId, salary)      ← cross-module │
 * │  3. departmentService.assignToDepartment(empId, deptId)  ← cross-module │
 * │                                                                           │
 * │  On success → /internal/saga-complete → employee.status = ACTIVE        │
 * │  On failure → cancelOnboardEmployee() + step compensations               │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Cross-module fields injected here:
 *   • PayrollService    → "payroll-service"    (ends in "Service" ✓, other module ✓)
 *   • DepartmentService → "department-service" (ends in "Service" ✓, other module ✓)
 *
 * Local fields:
 *   • EmployeeRepository → ends in "Repository" — NOT detected as cross-module ✓
 *
 * NOTE: EmployeeService is NOT injected here — that would be same-module *Service,
 *       which FractalX would falsely treat as a cross-module gRPC dependency.
 *       All local employee logic is performed directly via the repository.
 */
@Service
public class EmployeeModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    // → payroll-service (gRPC after decompose)
    private final PayrollServiceClient payrollServiceClient;

    // → department-service (gRPC after decompose)
    private final DepartmentServiceClient departmentServiceClient;

    // ── Local dependency ──────────────────────────────────────────────────────
    private final EmployeeRepository employeeRepository;

    public EmployeeModule(PayrollServiceClient payrollServiceClient, DepartmentServiceClient departmentServiceClient, EmployeeRepository employeeRepository, OutboxPublisher outboxPublisher) {
        this.payrollServiceClient = payrollServiceClient;
        this.departmentServiceClient = departmentServiceClient;
        this.employeeRepository = employeeRepository;
        this.outboxPublisher = outboxPublisher;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAGA 1: onboard-employee-saga
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Onboards a new employee in three transactional steps:
     *
     *   Step 1 (local)   — create Employee record with status=PENDING
     *   Step 2 (cross)   — payrollService.setupPayroll(employeeId, salary)
     *   Step 3 (cross)   — departmentService.assignToDepartment(employeeId, departmentId)
     *
     * FractalX transformation in generated employee-service:
     *   • The two cross-module calls are replaced with a single outbox event publish.
     *   • The orchestrator calls setupPayroll → assignToDepartment in order.
     *   • On success: POST /internal/saga-complete → status = "ACTIVE".
     *   • On failure: compensations fire in reverse order, then cancelOnboardEmployee().
     *
     * @param firstName    – standard Java type ✓
     * @param lastName     – standard Java type ✓
     * @param email        – standard Java type ✓
     * @param position     – standard Java type ✓
     * @param salary       – standard Java type (BigDecimal) ✓
     * @param departmentId – standard Java type ✓
     */
    @Transactional
    public Employee onboardEmployee(String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        // ── STEP 1: Local work — save PENDING employee ────────────────────────
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPosition(position);
        employee.setSalary(salary);
        employee.setDepartmentId(departmentId);
        employee.setHireDate(LocalDate.now());
        // FractalX saga-complete callback will change to ACTIVE
        employee.setStatus("PENDING");
        employee.setPayrollSetUp(false);
        employee = employeeRepository.save(employee);
        // captured — included in saga payload automatically
        Long employeeId = employee.getId();
        // Saga steps delegated to orchestrator via transactional outbox
        java.util.Map<String, Object> sagaPayload = new java.util.LinkedHashMap<>();
        sagaPayload.put("firstName", firstName);
        sagaPayload.put("lastName", lastName);
        sagaPayload.put("email", email);
        sagaPayload.put("position", position);
        sagaPayload.put("salary", salary);
        sagaPayload.put("departmentId", departmentId);
        sagaPayload.put("employeeId", employeeId);
        outboxPublisher.publish("onboard-employee-saga", String.valueOf(firstName), sagaPayload);
        // ── STEP 2: Cross-module call — sets up payroll record ────────────────
        // FractalX replaces this with outbox.publish("onboard-employee-saga", ...)
        // In the monolith the status is set here; in generated code the saga
        return employeeRepository.save(employee);
    }

    /**
     * Overall saga rollback — called on employee-service when the entire saga is FAILED.
     * Must have the SAME parameter signature as onboardEmployee().
     * Must be idempotent.
     */
    @Transactional
    public void cancelOnboardEmployee(String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        employeeRepository.findByEmailAndStatus(email, "PENDING").ifPresent(e -> {
            e.setStatus("CANCELLED");
            employeeRepository.save(e);
        });
    }

    private final OutboxPublisher outboxPublisher;
}
