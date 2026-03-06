package org.fractalx.admin.services;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Baked-in registry of all generated services and infrastructure services.
 * Populated at generation time by FractalX — no runtime discovery needed.
 */
@Component
public class ServiceMetaRegistry {

    public record ServiceMeta(
            String name, int port, int grpcPort, String type,
            List<String> dependencies, String packageName, String className) {}

    private static final List<ServiceMeta> SERVICES = List.of(
        new ServiceMeta("department-service", 8085, 18085, "microservice", List.of(), "com.example.hr.department", "com.example.hr.department.DepartmentModule"),
        new ServiceMeta("employee-service", 8081, 18081, "microservice", List.of("department-service", "payroll-service"), "com.example.hr.employee", "com.example.hr.employee.EmployeeModule"),
        new ServiceMeta("leave-service", 8083, 18083, "microservice", List.of("payroll-service", "employee-service"), "com.example.hr.leave", "com.example.hr.leave.LeaveModule"),
        new ServiceMeta("payroll-service", 8082, 18082, "microservice", List.of(), "com.example.hr.payroll", "com.example.hr.payroll.PayrollModule"),
        new ServiceMeta("recruitment-service", 8084, 18084, "microservice", List.of("department-service", "employee-service"), "com.example.hr.recruitment", "com.example.hr.recruitment.RecruitmentModule"),
        new ServiceMeta("fractalx-saga-orchestrator", 8099, 18099, "saga", List.of(), "", ""),
        new ServiceMeta("fractalx-registry", 8761, 0, "infrastructure", List.of(), "", ""),
        new ServiceMeta("api-gateway",       9999, 0, "infrastructure", List.of(), "", ""),
        new ServiceMeta("admin-service",     9090, 0, "infrastructure", List.of(), "", ""),
        new ServiceMeta("logger-service",    9099, 0, "infrastructure", List.of(), "", "")
    );

    public List<ServiceMeta> getAll()                    { return SERVICES; }
    public int               size()                      { return SERVICES.size(); }

    public Optional<ServiceMeta> findByName(String name) {
        return SERVICES.stream().filter(s -> s.name().equals(name)).findFirst();
    }

    public List<ServiceMeta> getByType(String type) {
        return SERVICES.stream().filter(s -> s.type().equals(type)).toList();
    }

    public long countMicroservices() {
        return SERVICES.stream().filter(s -> "microservice".equals(s.type())).count();
    }
}
