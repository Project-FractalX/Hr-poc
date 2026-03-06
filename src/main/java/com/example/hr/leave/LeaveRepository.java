package com.example.hr.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LeaveRepository extends JpaRepository<LeaveEntry, Long> {
    List<LeaveEntry> findByEmployeeId(Long employeeId);
    List<LeaveEntry> findByEmployeeIdAndStatus(Long employeeId, String status);
    Optional<LeaveEntry> findTopByEmployeeIdAndStatusOrderByRequestedAtDesc(Long employeeId, String status);
}
