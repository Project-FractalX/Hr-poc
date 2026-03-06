package com.example.hr.department;

import org.springframework.stereotype.Service;

/**
 * Department module boundary.
 *
 * This module is a "leaf" service — it has no cross-module dependencies of its own.
 * Other services (employee-service, recruitment-service) call it via gRPC after decomposition.
 *
 * Injects only a Repository — no *Service fields here, so FractalX won't generate any
 * spurious gRPC clients for this module.
 */
@Service
public class DepartmentModule {

    private final DepartmentRepository departmentRepository;

    public DepartmentModule(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }
}
