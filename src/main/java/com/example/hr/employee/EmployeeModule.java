package com.example.hr.employee;

import com.example.hr.department.DepartmentService;
import com.example.hr.payroll.PayrollService;
import org.fractalx.annotations.DecomposableModule;
import org.fractalx.annotations.DistributedSaga;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

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
@DecomposableModule(
    serviceName  = "employee-service",
    port         = 8081,
    ownedSchemas = {"employees"}
)
@Service
public class EmployeeModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    private final PayrollService    payrollService;     // → payroll-service (gRPC after decompose)
    private final DepartmentService departmentService;  // → department-service (gRPC after decompose)

    // ── Local dependency ──────────────────────────────────────────────────────
    private final EmployeeRepository employeeRepository;

    public EmployeeModule(PayrollService    payrollService,
                          DepartmentService departmentService,
                          EmployeeRepository employeeRepository) {
        this.payrollService    = payrollService;
        this.departmentService = departmentService;
        this.employeeRepository = employeeRepository;
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
    @DistributedSaga(
        sagaId             = "onboard-employee-saga",
        compensationMethod = "cancelOnboardEmployee",
        timeout            = 30_000,
        steps              = {"setupPayroll", "assignToDepartment"},
        description        = "Onboards a new employee: sets up payroll and assigns to department."
    )
    @Transactional
    public Employee onboardEmployee(String firstName, String lastName, String email,
                                    String position, BigDecimal salary, Long departmentId) {

        // ── STEP 1: Local work — save PENDING employee ────────────────────────
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPosition(position);
        employee.setSalary(salary);
        employee.setDepartmentId(departmentId);
        employee.setHireDate(LocalDate.now());
        employee.setStatus("PENDING");      // FractalX saga-complete callback will change to ACTIVE
        employee.setPayrollSetUp(false);
        employee = employeeRepository.save(employee);

        Long employeeId = employee.getId(); // captured — included in saga payload automatically

        // ── STEP 2: Cross-module call — sets up payroll record ────────────────
        // FractalX replaces this with outbox.publish("onboard-employee-saga", ...)
        // in the generated employee-service.
        payrollService.setupPayroll(employeeId, salary);

        // ── STEP 3: Cross-module call — increments department headcount ───────
        departmentService.assignToDepartment(employeeId, departmentId);

        // In the monolith the status is set here; in generated code the saga
        // completion callback sets it asynchronously.
        employee.setStatus("ACTIVE");
        employee.setPayrollSetUp(true);
        return employeeRepository.save(employee);
    }

    /**
     * Overall saga rollback — called on employee-service when the entire saga is FAILED.
     * Must have the SAME parameter signature as onboardEmployee().
     * Must be idempotent.
     */
    @Transactional
    public void cancelOnboardEmployee(String firstName, String lastName, String email,
                                       String position, BigDecimal salary, Long departmentId) {
        employeeRepository.findByEmailAndStatus(email, "PENDING").ifPresent(e -> {
            e.setStatus("CANCELLED");
            employeeRepository.save(e);
        });
    }
}
