package com.example.hr.recruitment;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Internal recruitment operations — not a cross-module dependency. */
@Service
public class RecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentService.class);
    private final CandidateRepository candidateRepository;

    public RecruitmentService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public Candidate apply(String firstName, String lastName, String email,
                           String position, Long departmentId, BigDecimal expectedSalary) {
        if (candidateRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Application already exists for: " + email);
        }
        Candidate c = new Candidate();
        c.setFirstName(firstName); c.setLastName(lastName); c.setEmail(email);
        c.setAppliedPosition(position); c.setTargetDepartmentId(departmentId);
        c.setOfferedSalary(expectedSalary); c.setStatus("APPLIED");
        log.info("[recruitment] New application from {} {}", firstName, lastName);
        return candidateRepository.save(c);
    }

    @Transactional
    public Candidate shortlist(Long candidateId) {
        Candidate c = requireCandidate(candidateId);
        c.setStatus("SHORTLISTED");
        return candidateRepository.save(c);
    }

    @Transactional
    public Candidate reject(Long candidateId) {
        Candidate c = requireCandidate(candidateId);
        if ("HIRED".equals(c.getStatus())) {
            throw new IllegalStateException("Cannot reject an already hired candidate.");
        }
        c.setStatus("REJECTED");
        return candidateRepository.save(c);
    }

    public Candidate findById(Long id) { return requireCandidate(id); }

    public List<Candidate> findByStatus(String status) {
        return candidateRepository.findByStatus(status);
    }

    public List<Candidate> findShortlisted() { return findByStatus("SHORTLISTED"); }

    private Candidate requireCandidate(Long id) {
        return candidateRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Candidate not found: " + id));
    }
}
