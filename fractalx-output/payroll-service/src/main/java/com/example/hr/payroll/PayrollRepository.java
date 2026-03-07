package com.example.hr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    Optional<PayrollRecord> findByEmployeeIdAndPayYearAndPayMonth(Long employeeId, Integer year, Integer month);
    List<PayrollRecord> findByEmployeeId(Long employeeId);
    List<PayrollRecord> findByPayYearAndPayMonth(Integer year, Integer month);
    Optional<PayrollRecord> findTopByEmployeeIdAndStatusOrderByCreatedAtDesc(Long employeeId, String status);
}
