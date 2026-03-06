package com.example.hr.department;

import com.example.hr.department.dto.CreateDepartmentRequest;
import com.example.hr.department.dto.DepartmentResponse;
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
@RequestMapping("/api/department")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest req) {
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.createDepartment(req.getCode(), req.getName(), req.getAnnualBudget())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> listActive() {
        return ResponseEntity.ok(departmentService.findAllActive().stream().map(DepartmentResponse::from).collect(Collectors.toList()));
    }
}
