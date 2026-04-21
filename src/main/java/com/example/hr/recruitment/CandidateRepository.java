package com.example.hr.recruitment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByEmail(String email);
    List<Candidate> findByStatus(String status);
    List<Candidate> findByTargetDepartmentId(Long departmentId);
    Optional<Candidate> findByIdAndStatus(Long id, String status);
}
