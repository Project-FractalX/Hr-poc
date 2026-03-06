package com.example.hr.leave;

import com.example.hr.leave.dto.ApproveLeaveRequest;
import com.example.hr.leave.dto.LeaveResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveModule leaveModule;

    private final LeaveService leaveService;

    public LeaveController(LeaveModule leaveModule, LeaveService leaveService) {
        this.leaveModule = leaveModule;
        this.leaveService = leaveService;
    }

    /**
     * Starts the approve-leave-saga.
     */
    @PostMapping("/approve")
    public ResponseEntity<LeaveResponse> approve(@Valid @RequestBody ApproveLeaveRequest req) {
        LeaveEntry entry = leaveModule.approveLeave(req.getEmployeeId(), req.getLeaveType(), req.getStartDate(), req.getEndDate(), req.getLeaveDays(), req.getReason());
        return ResponseEntity.ok(LeaveResponse.from(entry));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(LeaveResponse.from(leaveService.findById(id)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.findByEmployee(employeeId).stream().map(LeaveResponse::from).collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        leaveService.rejectLeave(id);
        return ResponseEntity.noContent().build();
    }
}
