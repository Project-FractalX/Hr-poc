package com.example.hr.payroll;

import org.springframework.stereotype.Service;

/**
 * Payroll module boundary.
 *
 * This is a "leaf" module — it owns its own data and exposes PayrollService
 * to other modules via cross-module calls. It has no upstream service dependencies.
 *
 * FractalX rule: only Repository types injected here — no *Service suffix types,
 * which would be mistaken for cross-module dependencies.
 */
@Service
public class PayrollModule {

    private final PayrollRepository payrollRepository;

    public PayrollModule(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }
}
