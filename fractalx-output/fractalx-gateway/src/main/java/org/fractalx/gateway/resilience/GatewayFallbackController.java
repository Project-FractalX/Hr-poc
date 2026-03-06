package org.fractalx.gateway.resilience;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {
    
    @GetMapping("/fallback/department-service")
    @PostMapping("/fallback/department-service")
    public ResponseEntity<Map<String, Object>> departmentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "service", "department-service",
                        "message", "The service is temporarily unavailable. Please retry shortly.",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @GetMapping("/fallback/employee-service")
    @PostMapping("/fallback/employee-service")
    public ResponseEntity<Map<String, Object>> employeeServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "service", "employee-service",
                        "message", "The service is temporarily unavailable. Please retry shortly.",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @GetMapping("/fallback/leave-service")
    @PostMapping("/fallback/leave-service")
    public ResponseEntity<Map<String, Object>> leaveServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "service", "leave-service",
                        "message", "The service is temporarily unavailable. Please retry shortly.",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @GetMapping("/fallback/payroll-service")
    @PostMapping("/fallback/payroll-service")
    public ResponseEntity<Map<String, Object>> payrollServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "service", "payroll-service",
                        "message", "The service is temporarily unavailable. Please retry shortly.",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @GetMapping("/fallback/recruitment-service")
    @PostMapping("/fallback/recruitment-service")
    public ResponseEntity<Map<String, Object>> recruitmentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "service", "recruitment-service",
                        "message", "The service is temporarily unavailable. Please retry shortly.",
                        "timestamp", System.currentTimeMillis()
                ));
    }

}
