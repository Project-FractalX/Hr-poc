package com.example.hr.recruitment;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import org.fractalx.generated.recruitmentservice.outbox.OutboxPublisher;

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
@Service
public class RecruitmentModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    // → employee-service   (gRPC post decompose)
    private final EmployeeServiceClient employeeServiceClient;

    // → department-service (gRPC post decompose)
    private final DepartmentServiceClient departmentServiceClient;

    // ── Local dependency ──────────────────────────────────────────────────────
    private final CandidateRepository candidateRepository;

    public RecruitmentModule(EmployeeServiceClient employeeServiceClient, DepartmentServiceClient departmentServiceClient, CandidateRepository candidateRepository, OutboxPublisher outboxPublisher) {
        this.employeeServiceClient = employeeServiceClient;
        this.departmentServiceClient = departmentServiceClient;
        this.candidateRepository = candidateRepository;
        this.outboxPublisher = outboxPublisher;
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
    @Transactional
    public Candidate hireEmployee(Long candidateId, String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        // ── STEP 1: Local work — mark candidate as PENDING ────────────────────
        Candidate candidate = candidateRepository.findById(candidateId).orElseThrow(() -> new EntityNotFoundException("Candidate not found: " + candidateId));
        // saga-complete → "HIRED"
        candidate.setApplicationStatus("PENDING");
        candidate.setOfferedSalary(salary);
        candidate.setTargetDepartmentId(departmentId);
        candidate = candidateRepository.save(candidate);
        // Saga steps delegated to orchestrator via transactional outbox
        java.util.Map<String, Object> sagaPayload = new java.util.LinkedHashMap<>();
        sagaPayload.put("candidateId", candidateId);
        sagaPayload.put("firstName", firstName);
        sagaPayload.put("lastName", lastName);
        sagaPayload.put("email", email);
        sagaPayload.put("position", position);
        sagaPayload.put("salary", salary);
        sagaPayload.put("departmentId", departmentId);
        outboxPublisher.publish("hire-employee-saga", String.valueOf(candidateId), sagaPayload);
        // ── STEP 2: Cross-module — create the employee record ─────────────────
        return candidateRepository.save(candidate);
    }

    /**
     * Overall saga rollback — called on recruitment-service when hire-employee-saga FAILS.
     * Same parameters as hireEmployee(). Must be idempotent.
     */
    @Transactional
    public void cancelHireEmployee(Long candidateId, String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        candidateRepository.findByIdAndApplicationStatus(candidateId, "PENDING").ifPresent(c -> {
            c.setApplicationStatus("CANCELLED");
            candidateRepository.save(c);
        });
    }

    private final OutboxPublisher outboxPublisher;
}
