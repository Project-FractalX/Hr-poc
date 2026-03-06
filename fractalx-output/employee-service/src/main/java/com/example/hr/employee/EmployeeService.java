package com.example.hr.employee;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.fractalx.netscope.server.annotation.NetworkPublic;

/**
 * EmployeeService — called cross-module by:
 *   • LeaveModule       (approve-leave-saga  step 1: markOnLeave / validateEmployee)
 *   • RecruitmentModule (hire-employee-saga  step 1: createEmployee)
 *
 * FractalX will generate gRPC stubs for these methods. Compensation methods follow
 * the "cancel" + CapitalisedName convention.
 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ── Cross-module saga step: called by LeaveModule ─────────────────────────
    /**
     * Validates that the employee is active and marks them as ON_LEAVE.
     * Called as step 1 of approve-leave-saga.
     *
     * @param employeeId the employee requesting leave
     */
    @Transactional
    @NetworkPublic()
    public void markOnLeave(Long employeeId) {
        log.info("[employee] Marking employee {} as ON_LEAVE", employeeId);
        Employee e = requireActiveEmployee(employeeId);
        e.setStatus("ON_LEAVE");
        employeeRepository.save(e);
    }

    /**
     * Compensation for markOnLeave — resets the employee back to ACTIVE.
     * Idempotent: safe to call multiple times.
     */
    @Transactional
    @NetworkPublic()
    public void cancelMarkOnLeave(Long employeeId) {
        log.warn("[employee] COMPENSATING: reverting ON_LEAVE for employee {}", employeeId);
        employeeRepository.findById(employeeId).ifPresent(e -> {
            if ("ON_LEAVE".equals(e.getStatus())) {
                e.setStatus("ACTIVE");
                employeeRepository.save(e);
            }
        });
    }

    // ── Cross-module saga step: called by RecruitmentModule ──────────────────
    /**
     * Creates a new employee record from a hired candidate.
     * Called as step 1 of hire-employee-saga.
     *
     * All parameters are standard Java types — required by FractalX saga orchestrator.
     *
     * @param firstName    first name of the new hire
     * @param lastName     last name of the new hire
     * @param email        work email
     * @param position     job title/position
     * @param salary       agreed salary
     * @param departmentId department the new hire will join
     * @return the newly created Employee (status = ACTIVE)
     */
    @Transactional
    @NetworkPublic()
    public Employee createEmployee(String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        log.info("[employee] Creating employee record for {} {}", firstName, lastName);
        if (employeeRepository.existsByEmail(email)) {
            throw new IllegalStateException("Employee with email already exists: " + email);
        }
        Employee e = new Employee();
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setEmail(email);
        e.setPosition(position);
        e.setSalary(salary);
        e.setDepartmentId(departmentId);
        e.setStatus("ACTIVE");
        e.setHireDate(LocalDate.now());
        e.setPayrollSetUp(false);
        return employeeRepository.save(e);
    }

    /**
     * Compensation for createEmployee — marks the employee as CANCELLED if the hire saga fails.
     * Idempotent.
     */
    @Transactional
    @NetworkPublic()
    public void cancelCreateEmployee(String firstName, String lastName, String email, String position, BigDecimal salary, Long departmentId) {
        log.warn("[employee] COMPENSATING: cancelling employee record for {}", email);
        employeeRepository.findByEmail(email).ifPresent(e -> {
            e.setStatus("CANCELLED");
            employeeRepository.save(e);
        });
    }

    // ── Non-saga operations ───────────────────────────────────────────────────
    @NetworkPublic()
    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
    }

    @NetworkPublic()
    public boolean exists(Long id) {
        return employeeRepository.existsById(id);
    }

    @Transactional
    @NetworkPublic()
    public void terminate(Long employeeId) {
        Employee e = requireActiveEmployee(employeeId);
        e.setStatus("TERMINATED");
        employeeRepository.save(e);
    }

    @NetworkPublic()
    public List<Employee> findByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }

    @NetworkPublic()
    public List<Employee> findActive() {
        return employeeRepository.findByStatus("ACTIVE");
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private Employee requireActiveEmployee(Long id) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
        if (!"ACTIVE".equals(e.getStatus())) {
            throw new IllegalStateException("Employee " + id + " is not ACTIVE (status=" + e.getStatus() + ")");
        }
        return e;
    }
}
