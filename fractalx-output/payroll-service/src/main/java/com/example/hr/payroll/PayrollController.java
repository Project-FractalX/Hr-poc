package com.example.hr.payroll;

import com.example.hr.payroll.dto.PayrollResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.findByEmployee(employeeId).stream().map(PayrollResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/period/{year}/{month}")
    public ResponseEntity<List<PayrollResponse>> getByPeriod(@PathVariable Integer year, @PathVariable Integer month) {
        return ResponseEntity.ok(payrollService.findByPeriod(year, month).stream().map(PayrollResponse::from).collect(Collectors.toList()));
    }
}
