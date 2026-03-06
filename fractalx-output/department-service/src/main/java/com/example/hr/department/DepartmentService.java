package com.example.hr.department;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.fractalx.netscope.server.annotation.NetworkPublic;

/**
 * DepartmentService — called cross-module by:
 *   • EmployeeModule  (onboard-employee-saga step 2: assignToDepartment)
 *   • RecruitmentModule (hire-employee-saga step 2: incrementHeadcount)
 *
 * FractalX will generate gRPC stubs so other services can call these methods
 * after decomposition.  Compensation methods follow the "cancel" + CapitalizedForwardName
 * convention required by FractalX.
 */
@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // ── Cross-module saga step: called by EmployeeModule ─────────────────────
    /**
     * Assigns an employee to a department by recording the assignment and
     * bumping the headcount.  Called as step 2 of onboard-employee-saga.
     */
    @Transactional
    @NetworkPublic()
    public void assignToDepartment(Long employeeId, Long departmentId) {
        log.info("[dept] Assigning employee {} to department {}", employeeId, departmentId);
        Department dept = requireDepartment(departmentId);
        dept.setHeadcount(dept.getHeadcount() + 1);
        departmentRepository.save(dept);
        log.info("[dept] Department {} headcount is now {}", dept.getCode(), dept.getHeadcount());
    }

    /**
     * Compensation for assignToDepartment — rolls back headcount increment.
     * Called when a LATER step in onboard-employee-saga fails.
     */
    @Transactional
    @NetworkPublic()
    public void cancelAssignToDepartment(Long employeeId, Long departmentId) {
        log.warn("[dept] COMPENSATING: reverting department assignment for employee {}", employeeId);
        departmentRepository.findById(departmentId).ifPresent(dept -> {
            if (dept.getHeadcount() > 0) {
                dept.setHeadcount(dept.getHeadcount() - 1);
                departmentRepository.save(dept);
            }
        });
    }

    // ── Cross-module saga step: called by RecruitmentModule ──────────────────
    /**
     * Increments the headcount of a department when a candidate is officially hired.
     * Called as step 2 of hire-employee-saga.
     */
    @Transactional
    @NetworkPublic()
    public void incrementHeadcount(Long departmentId) {
        log.info("[dept] Incrementing headcount for department {}", departmentId);
        Department dept = requireDepartment(departmentId);
        dept.setHeadcount(dept.getHeadcount() + 1);
        departmentRepository.save(dept);
    }

    /**
     * Compensation for incrementHeadcount — rolls back if hire saga fails.
     */
    @Transactional
    @NetworkPublic()
    public void cancelIncrementHeadcount(Long departmentId) {
        log.warn("[dept] COMPENSATING: decrementing headcount for department {}", departmentId);
        departmentRepository.findById(departmentId).ifPresent(dept -> {
            if (dept.getHeadcount() > 0) {
                dept.setHeadcount(dept.getHeadcount() - 1);
                departmentRepository.save(dept);
            }
        });
    }

    // ── Non-saga read operations ──────────────────────────────────────────────
    @NetworkPublic()
    public Department findById(Long id) {
        return requireDepartment(id);
    }

    @NetworkPublic()
    public boolean exists(Long id) {
        return departmentRepository.existsById(id);
    }

    @Transactional
    @NetworkPublic()
    public Department createDepartment(String code, String name, java.math.BigDecimal budget) {
        if (departmentRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Department code already exists: " + code);
        }
        Department dept = new Department();
        dept.setCode(code.toUpperCase());
        dept.setName(name);
        dept.setAnnualBudget(budget);
        dept.setHeadcount(0);
        dept.setStatus("ACTIVE");
        return departmentRepository.save(dept);
    }

    @NetworkPublic()
    public java.util.List<Department> findAllActive() {
        return departmentRepository.findByStatus("ACTIVE");
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Department not found: " + id));
    }
}
