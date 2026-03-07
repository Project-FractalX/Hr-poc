package com.example.hr.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmailAndStatus(String email, String status);
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByStatus(String status);
    boolean existsByEmail(String email);
}
