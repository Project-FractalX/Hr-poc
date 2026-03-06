package com.example.hr.recruitment;

import com.example.hr.recruitment.dto.ApplyRequest;
import com.example.hr.recruitment.dto.CandidateResponse;
import com.example.hr.recruitment.dto.HireCandidateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentController {

    private final RecruitmentModule  recruitmentModule;
    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentModule recruitmentModule,
                                  RecruitmentService recruitmentService) {
        this.recruitmentModule  = recruitmentModule;
        this.recruitmentService = recruitmentService;
    }

    /** Submit a job application. */
    @PostMapping("/apply")
    public ResponseEntity<CandidateResponse> apply(@Valid @RequestBody ApplyRequest req) {
        return ResponseEntity.ok(CandidateResponse.from(
            recruitmentService.apply(req.getFirstName(), req.getLastName(), req.getEmail(),
                req.getPosition(), req.getDepartmentId(), req.getExpectedSalary())));
    }

    /** Shortlist a candidate (no saga — single-service state change). */
    @PatchMapping("/{id}/shortlist")
    public ResponseEntity<CandidateResponse> shortlist(@PathVariable Long id) {
        return ResponseEntity.ok(CandidateResponse.from(recruitmentService.shortlist(id)));
    }

    /** Reject an application (no saga — single-service state change). */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<CandidateResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(CandidateResponse.from(recruitmentService.reject(id)));
    }

    /** Starts the hire-employee-saga. */
    @PostMapping("/hire")
    public ResponseEntity<CandidateResponse> hire(@Valid @RequestBody HireCandidateRequest req) {
        return ResponseEntity.ok(CandidateResponse.from(
            recruitmentModule.hireEmployee(
                req.getCandidateId(), req.getFirstName(), req.getLastName(),
                req.getEmail(), req.getPosition(), req.getSalary(), req.getDepartmentId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(CandidateResponse.from(recruitmentService.findById(id)));
    }

    @GetMapping("/shortlisted")
    public ResponseEntity<List<CandidateResponse>> shortlisted() {
        return ResponseEntity.ok(recruitmentService.findShortlisted().stream()
            .map(CandidateResponse::from).collect(Collectors.toList()));
    }
}
