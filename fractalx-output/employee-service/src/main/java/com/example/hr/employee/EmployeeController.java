package com.example.hr.employee;

import com.example.hr.employee.dto.EmployeeResponse;
import com.example.hr.employee.dto.OnboardEmployeeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeModule employeeModule;

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeModule employeeModule, EmployeeService employeeService) {
        this.employeeModule = employeeModule;
        this.employeeService = employeeService;
    }

    /**
     * Starts the onboard-employee-saga (creates employee + sets up payroll + assigns dept).
     */
    @PostMapping("/onboard")
    public ResponseEntity<EmployeeResponse> onboard(@Valid @RequestBody OnboardEmployeeRequest req) {
        Employee e = employeeModule.onboardEmployee(req.getFirstName(), req.getLastName(), req.getEmail(), req.getPosition(), req.getSalary(), req.getDepartmentId());
        return ResponseEntity.ok(EmployeeResponse.from(e));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listActive() {
        return ResponseEntity.ok(employeeService.findActive().stream().map(EmployeeResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<EmployeeResponse>> getByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(employeeService.findByDepartment(departmentId).stream().map(EmployeeResponse::from).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}/terminate")
    public ResponseEntity<Void> terminate(@PathVariable Long id) {
        employeeService.terminate(id);
        return ResponseEntity.noContent().build();
    }
}
