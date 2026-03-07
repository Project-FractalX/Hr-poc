package com.example.hr.leave;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Internal leave service for read operations and controller use. */
@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);
    private final LeaveRepository leaveRepository;

    public LeaveService(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    public LeaveEntry findById(Long id) {
        return leaveRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Leave entry not found: " + id));
    }

    public List<LeaveEntry> findByEmployee(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<LeaveEntry> findApprovedByEmployee(Long employeeId) {
        return leaveRepository.findByEmployeeIdAndStatus(employeeId, "APPROVED");
    }

    @Transactional
    public void rejectLeave(Long leaveId) {
        LeaveEntry entry = findById(leaveId);
        if (!"PENDING".equals(entry.getStatus())) {
            throw new IllegalStateException("Only PENDING leave can be rejected");
        }
        entry.setStatus("REJECTED");
        leaveRepository.save(entry);
        log.info("[leave] Rejected leave entry {}", leaveId);
    }
}
