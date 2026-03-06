package com.example.hr.payroll;

import org.fractalx.annotations.ServiceBoundary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PayrollService — called cross-module by:
 *   • EmployeeModule (onboard-employee-saga step 1: setupPayroll)
 *   • LeaveModule    (approve-leave-saga  step 2: deductLeave)
 *
 * All saga step methods have a corresponding compensation method named
 * "cancel" + CapitalisedForwardMethodName — FractalX detects these automatically.
 */
@Service
@ServiceBoundary(
    allowedCallers = {"employee-service", "leave-service"},
    strict = true
)
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;

    public PayrollService(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    // ── Cross-module saga step: called by EmployeeModule ─────────────────────

    /**
     * Creates the initial payroll record for a newly onboarded employee.
     * Called as step 1 of onboard-employee-saga.
     *
     * @param employeeId the ID of the newly created employee (in PENDING state)
     * @param salary     the agreed annual salary
     * @return the saved payroll record
     */
    @Transactional
    public PayrollRecord setupPayroll(Long employeeId, BigDecimal salary) {
        log.info("[payroll] Setting up payroll for employee {} at salary {}", employeeId, salary);
        LocalDate now = LocalDate.now();
        PayrollRecord record = new PayrollRecord();
        record.setEmployeeId(employeeId);
        record.setPayYear(now.getYear());
        record.setPayMonth(now.getMonthValue());
        record.setBasicSalary(salary);
        record.setDeductions(BigDecimal.ZERO);
        record.setNetSalary(salary);
        record.setLeaveDaysDeducted(0);
        record.setStatus("ACTIVE");
        return payrollRepository.save(record);
    }

    /**
     * Compensation for setupPayroll — cancels the payroll record if the saga rolls back.
     * Idempotent: safe to call multiple times.
     */
    @Transactional
    public void cancelSetupPayroll(Long employeeId, BigDecimal salary) {
        log.warn("[payroll] COMPENSATING: cancelling payroll for employee {}", employeeId);
        payrollRepository
            .findTopByEmployeeIdAndStatusOrderByCreatedAtDesc(employeeId, "ACTIVE")
            .ifPresent(r -> {
                r.setStatus("CANCELLED");
                payrollRepository.save(r);
            });
    }

    // ── Cross-module saga step: called by LeaveModule ─────────────────────────

    /**
     * Deducts approved leave days from the employee's current-month payroll.
     * Called as step 2 of approve-leave-saga.
     *
     * @param employeeId the employee taking leave
     * @param leaveDays  number of working days of leave approved
     */
    @Transactional
    public void deductLeave(Long employeeId, Integer leaveDays) {
        log.info("[payroll] Deducting {} leave days for employee {}", leaveDays, employeeId);
        LocalDate now = LocalDate.now();
        payrollRepository
            .findByEmployeeIdAndPayYearAndPayMonth(employeeId, now.getYear(), now.getMonthValue())
            .ifPresent(record -> {
                // Daily rate = monthly salary / 22 working days
                BigDecimal dailyRate = record.getBasicSalary().divide(BigDecimal.valueOf(22), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal deduction = dailyRate.multiply(BigDecimal.valueOf(leaveDays));
                record.setDeductions(record.getDeductions().add(deduction));
                record.setNetSalary(record.getBasicSalary().subtract(record.getDeductions()));
                record.setLeaveDaysDeducted(record.getLeaveDaysDeducted() + leaveDays);
                payrollRepository.save(record);
            });
    }

    /**
     * Compensation for deductLeave — restores the salary deduction if leave approval rolls back.
     * Idempotent: safe to call multiple times.
     */
    @Transactional
    public void cancelDeductLeave(Long employeeId, Integer leaveDays) {
        log.warn("[payroll] COMPENSATING: restoring {} leave deduction for employee {}", leaveDays, employeeId);
        LocalDate now = LocalDate.now();
        payrollRepository
            .findByEmployeeIdAndPayYearAndPayMonth(employeeId, now.getYear(), now.getMonthValue())
            .ifPresent(record -> {
                BigDecimal dailyRate = record.getBasicSalary().divide(BigDecimal.valueOf(22), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal restoration = dailyRate.multiply(BigDecimal.valueOf(leaveDays));
                record.setDeductions(record.getDeductions().subtract(restoration).max(BigDecimal.ZERO));
                record.setNetSalary(record.getBasicSalary().subtract(record.getDeductions()));
                record.setLeaveDaysDeducted(Math.max(0, record.getLeaveDaysDeducted() - leaveDays));
                payrollRepository.save(record);
            });
    }

    // ── Non-saga operations ───────────────────────────────────────────────────

    public List<PayrollRecord> findByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    public List<PayrollRecord> findByPeriod(Integer year, Integer month) {
        return payrollRepository.findByPayYearAndPayMonth(year, month);
    }
}
