package com.example.hr.leave;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import org.fractalx.generated.leaveservice.outbox.OutboxPublisher;

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
@Service
public class LeaveModule {

    // ── Cross-module dependencies ─────────────────────────────────────────────
    // → employee-service (gRPC after decompose)
    private final EmployeeServiceClient employeeServiceClient;

    // → payroll-service  (gRPC after decompose)
    private final PayrollServiceClient payrollServiceClient;

    // ── Local dependency ──────────────────────────────────────────────────────
    private final LeaveRepository leaveRepository;

    public LeaveModule(EmployeeServiceClient employeeServiceClient, PayrollServiceClient payrollServiceClient, LeaveRepository leaveRepository, OutboxPublisher outboxPublisher) {
        this.employeeServiceClient = employeeServiceClient;
        this.payrollServiceClient = payrollServiceClient;
        this.leaveRepository = leaveRepository;
        this.outboxPublisher = outboxPublisher;
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
    @Transactional
    public LeaveEntry approveLeave(Long employeeId, String leaveType, String startDate, String endDate, Integer leaveDays, String reason) {
        // ── STEP 1: Local work — save PENDING leave entry ─────────────────────
        LeaveEntry entry = new LeaveEntry();
        entry.setEmployeeId(employeeId);
        entry.setLeaveType(leaveType);
        entry.setStartDate(LocalDate.parse(startDate));
        entry.setEndDate(LocalDate.parse(endDate));
        entry.setLeaveDays(leaveDays);
        entry.setReason(reason);
        // saga-complete callback → "APPROVED"
        entry.setStatus("PENDING");
        entry = leaveRepository.save(entry);
        // captured in saga payload
        Long leaveId = entry.getId();
        // Saga steps delegated to orchestrator via transactional outbox
        java.util.Map<String, Object> sagaPayload = new java.util.LinkedHashMap<>();
        sagaPayload.put("employeeId", employeeId);
        sagaPayload.put("leaveType", leaveType);
        sagaPayload.put("startDate", startDate);
        sagaPayload.put("endDate", endDate);
        sagaPayload.put("leaveDays", leaveDays);
        sagaPayload.put("reason", reason);
        outboxPublisher.publish("approve-leave-saga", String.valueOf(employeeId), sagaPayload);
        // ── STEP 2: Cross-module — mark employee as ON_LEAVE ─────────────────
        return leaveRepository.save(entry);
    }

    /**
     * Overall saga rollback — called on leave-service when approve-leave-saga FAILS.
     * Same parameters as approveLeave(). Must be idempotent.
     */
    @Transactional
    public void cancelApproveLeave(Long employeeId, String leaveType, String startDate, String endDate, Integer leaveDays, String reason) {
        leaveRepository.findTopByEmployeeIdAndStatusOrderByRequestedAtDesc(employeeId, "PENDING").ifPresent(entry -> {
            entry.setStatus("CANCELLED");
            leaveRepository.save(entry);
        });
    }

    private final OutboxPublisher outboxPublisher;
}
