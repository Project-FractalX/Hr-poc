package com.example.hr.recruitment;

import com.example.hr.department.DepartmentService;
import com.example.hr.employee.EmployeeService;
import jakarta.persistence.EntityNotFoundException;
import org.fractalx.annotations.DecomposableModule;
import org.fractalx.annotations.DistributedSaga;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Recruitment module boundary.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SAGA: hire-employee-saga                                                │
 * │                                                                           │
 * │  1. Mark Candidate(status=PENDING) locally                               │
 * │  2. employeeService.createEmployee(firstName, lastName, email,           │
 * │                                    position, salary, deptId) ← cross    │
 * │  3. departmentService.incrementHeadcount(departmentId)       ← cross    │
 * │                                                                           │
 * │  On success → /internal/saga-complete → candidate.status = HIRED        │
 * │  On failure → cancelHireEmployee() + step compensations in reverse      │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Cross-module fields:
 *   • EmployeeService   → "employee-service"   (ends in "Service" ✓, other module ✓)
 *   • DepartmentService → "department-service" (ends in "Service" ✓, other module ✓)
 *
 * Local fields:
 *   • CandidateRepository → ends in "Repository" — NOT detected as cross-module ✓
 */
@DecomposableModule(
    serviceName  = "recruitment-service",
    port         = 8084,
    ownedSchemas = {"candidates"}
)
@Service
public class RecruitmentModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    private final EmployeeService   employeeService;    // → employee-service   (gRPC post decompose)
    private final DepartmentService departmentService;  // → department-service (gRPC post decompose)

    // ── Local dependency ──────────────────────────────────────────────────────
    private final CandidateRepository candidateRepository;

    public RecruitmentModule(EmployeeService   employeeService,
                              DepartmentService departmentService,
                              CandidateRepository candidateRepository) {
        this.employeeService      = employeeService;
        this.departmentService    = departmentService;
        this.candidateRepository  = candidateRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAGA 3: hire-employee-saga
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Formally hires a shortlisted candidate:
     *
     *   Step 1 (local)  — mark candidate as PENDING
     *   Step 2 (cross)  — employeeService.createEmployee(firstName, lastName, email, position, salary, departmentId)
     *   Step 3 (cross)  — departmentService.incrementHeadcount(departmentId)
     *
     * All parameters are standard Java types — required by FractalX saga orchestrator.
     *
     * @param candidateId  – Long ✓
     * @param firstName    – String ✓  (could differ from application name)
     * @param lastName     – String ✓
     * @param email        – String ✓  (official work email)
     * @param position     – String ✓  (finalised job title)
     * @param salary       – BigDecimal ✓ (agreed salary)
     * @param departmentId – Long ✓
     */
    @DistributedSaga(
        sagaId             = "hire-employee-saga",
        compensationMethod = "cancelHireEmployee",
        timeout            = 30_000,
        steps              = {"createEmployee", "incrementHeadcount"},
        description        = "Hires a candidate: creates employee record and updates department headcount.",
        successStatus      = "HIRED",
        failureStatus      = "CANCELLED"
    )
    @Transactional
    public Candidate hireEmployee(Long candidateId, String firstName, String lastName,
                                  String email, String position,
                                  BigDecimal salary, Long departmentId) {

        // ── STEP 1: Local work — mark candidate as PENDING ────────────────────
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new EntityNotFoundException("Candidate not found: " + candidateId));

        candidate.setStatus("PENDING");  // saga-complete → "HIRED"
        candidate.setOfferedSalary(salary);
        candidate.setTargetDepartmentId(departmentId);
        candidate = candidateRepository.save(candidate);

        // ── STEP 2: Cross-module — create the employee record ─────────────────
        // FractalX replaces with outbox.publish("hire-employee-saga", ...) in generated code.
        employeeService.createEmployee(firstName, lastName, email, position, salary, departmentId);

        // ── STEP 3: Cross-module — update department headcount ────────────────
        departmentService.incrementHeadcount(departmentId);

        // Monolith only — final state; generated code gets this via callback.
        candidate.setStatus("HIRED");
        return candidateRepository.save(candidate);
    }

    /**
     * Overall saga rollback — called on recruitment-service when hire-employee-saga FAILS.
     * Same parameters as hireEmployee(). Must be idempotent.
     */
    @Transactional
    public void cancelHireEmployee(Long candidateId, String firstName, String lastName,
                                    String email, String position,
                                    BigDecimal salary, Long departmentId) {
        candidateRepository.findByIdAndStatus(candidateId, "PENDING")
            .ifPresent(c -> {
                c.setStatus("CANCELLED");
                candidateRepository.save(c);
            });
    }
}
