package com.example.hr;

import com.example.hr.department.Department;
import com.example.hr.department.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds reference data on startup so the API is immediately usable.
 * Creates 4 departments with realistic budget and headcount values.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DepartmentRepository departmentRepository;

    public DataInitializer(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (departmentRepository.count() > 0) return;   // idempotent

        seed("ENG",  "Engineering",         new BigDecimal("5000000"));
        seed("HR",   "Human Resources",     new BigDecimal("1500000"));
        seed("FIN",  "Finance",             new BigDecimal("2000000"));
        seed("MKT",  "Marketing",           new BigDecimal("1800000"));

        log.info("[init] Seeded {} departments", departmentRepository.count());
    }

    private void seed(String code, String name, BigDecimal budget) {
        Department d = new Department();
        d.setCode(code); d.setName(name);
        d.setAnnualBudget(budget); d.setHeadcount(0);
        d.setStatus("ACTIVE");
        departmentRepository.save(d);
    }
}
