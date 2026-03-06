package com.example.hr.leave;

import com.example.hr.employee.EmployeeService;
import com.example.hr.payroll.PayrollService;
import org.fractalx.annotations.DecomposableModule;
import org.fractalx.annotations.DistributedSaga;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Leave module boundary.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  SAGA: approve-leave-saga                                                │
 * │                                                                           │
 * │  1. Save LeaveEntry(status=PENDING) locally                              │
 * │  2. employeeService.markOnLeave(employeeId)          ← cross-module      │
 * │  3. payrollService.deductLeave(employeeId, leaveDays) ← cross-module     │
 * │                                                                           │
 * │  On success → /internal/saga-complete → leaveEntry.status = APPROVED    │
 * │  On failure → cancelApproveLeave() + step compensations in reverse      │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Cross-module fields:
 *   • EmployeeService → "employee-service" (ends in "Service" ✓, other module ✓)
 *   • PayrollService  → "payroll-service"  (ends in "Service" ✓, other module ✓)
 *
 * Local fields:
 *   • LeaveRepository → ends in "Repository" — NOT detected as cross-module ✓
 */
@DecomposableModule(
    serviceName  = "leave-service",
    port         = 8083,
    ownedSchemas = {"leave_entries"}
)
@Service
public class LeaveModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    private final EmployeeService employeeService;  // → employee-service (gRPC after decompose)
    private final PayrollService  payrollService;   // → payroll-service  (gRPC after decompose)

    // ── Local dependency ──────────────────────────────────────────────────────
    private final LeaveRepository leaveRepository;

    public LeaveModule(EmployeeService employeeService,
                       PayrollService  payrollService,
                       LeaveRepository leaveRepository) {
        this.employeeService = employeeService;
        this.payrollService  = payrollService;
        this.leaveRepository = leaveRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAGA 2: approve-leave-saga
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Approves a leave request in three steps:
     *
     *   Step 1 (local)  — save LeaveEntry with status=PENDING
     *   Step 2 (cross)  — employeeService.markOnLeave(employeeId)
     *   Step 3 (cross)  — payrollService.deductLeave(employeeId, leaveDays)
     *
     * @param employeeId  – Long ✓
     * @param leaveType   – String ✓ (ANNUAL | SICK | MATERNITY | PATERNITY | UNPAID)
     * @param startDate   – String ISO date ✓ (parsed locally)
     * @param endDate     – String ISO date ✓
     * @param leaveDays   – Integer ✓ (caller pre-computes working days)
     * @param reason      – String ✓
     */
    @DistributedSaga(
        sagaId             = "approve-leave-saga",
        compensationMethod = "cancelApproveLeave",
        timeout            = 30_000,
        steps              = {"markOnLeave", "deductLeave"},
        description        = "Approves employee leave: marks employee on leave and deducts pay."
    )
    @Transactional
    public LeaveEntry approveLeave(Long employeeId, String leaveType,
                                   String startDate, String endDate,
                                   Integer leaveDays, String reason) {

        // ── STEP 1: Local work — save PENDING leave entry ─────────────────────
        LeaveEntry entry = new LeaveEntry();
        entry.setEmployeeId(employeeId);
        entry.setLeaveType(leaveType);
        entry.setStartDate(LocalDate.parse(startDate));
        entry.setEndDate(LocalDate.parse(endDate));
        entry.setLeaveDays(leaveDays);
        entry.setReason(reason);
        entry.setStatus("PENDING");     // saga-complete callback → "APPROVED"
        entry = leaveRepository.save(entry);

        Long leaveId = entry.getId();   // captured in saga payload

        // ── STEP 2: Cross-module — mark employee as ON_LEAVE ─────────────────
        // FractalX replaces with outbox.publish("approve-leave-saga", ...) in generated code.
        employeeService.markOnLeave(employeeId);

        // ── STEP 3: Cross-module — deduct leave days from payroll ─────────────
        payrollService.deductLeave(employeeId, leaveDays);

        // Monolith only — set final state; generated code receives this via callback.
        entry.setStatus("APPROVED");
        return leaveRepository.save(entry);
    }

    /**
     * Overall saga rollback — called on leave-service when approve-leave-saga FAILS.
     * Same parameters as approveLeave(). Must be idempotent.
     */
    @Transactional
    public void cancelApproveLeave(Long employeeId, String leaveType,
                                    String startDate, String endDate,
                                    Integer leaveDays, String reason) {
        leaveRepository
            .findTopByEmployeeIdAndStatusOrderByRequestedAtDesc(employeeId, "PENDING")
            .ifPresent(entry -> {
                entry.setStatus("CANCELLED");
                leaveRepository.save(entry);
            });
    }
}
