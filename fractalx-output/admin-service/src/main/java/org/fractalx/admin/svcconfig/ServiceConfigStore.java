package org.fractalx.admin.svcconfig;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Baked-in configuration registry for all generated services.
 * Each entry captures port assignments, package info, environment variables,
 * schema ownership, and feature flags derived at generation time.
 */
@Component
public class ServiceConfigStore {

    public record ServiceConfig(
            String          name,
            int             httpPort,
            int             grpcPort,
            String          packageName,
            String          className,
            boolean         hasOutbox,
            boolean         hasSaga,
            List<String>    ownedSchemas,
            Map<String, String> envVars) {}

    private static final List<ServiceConfig> CONFIGS = List.of(
        new ServiceConfig("department-service", 8085, 18085, "com.example.hr.department", "com.example.hr.department.DepartmentModule", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker", "OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4317", "OTEL_SERVICE_NAME","department-service", "FRACTALX_REGISTRY_URL","http://localhost:8761", "FRACTALX_LOGGER_URL","http://localhost:9099")),
        new ServiceConfig("employee-service", 8081, 18081, "com.example.hr.employee", "com.example.hr.employee.EmployeeModule", true, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker", "OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4317", "OTEL_SERVICE_NAME","employee-service", "FRACTALX_REGISTRY_URL","http://localhost:8761", "FRACTALX_LOGGER_URL","http://localhost:9099", "DEPARTMENT_SERVICE_HOST","department-service", "PAYROLL_SERVICE_HOST","payroll-service")),
        new ServiceConfig("leave-service", 8083, 18083, "com.example.hr.leave", "com.example.hr.leave.LeaveModule", true, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker", "OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4317", "OTEL_SERVICE_NAME","leave-service", "FRACTALX_REGISTRY_URL","http://localhost:8761", "FRACTALX_LOGGER_URL","http://localhost:9099", "PAYROLL_SERVICE_HOST","payroll-service", "EMPLOYEE_SERVICE_HOST","employee-service")),
        new ServiceConfig("payroll-service", 8082, 18082, "com.example.hr.payroll", "com.example.hr.payroll.PayrollModule", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker", "OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4317", "OTEL_SERVICE_NAME","payroll-service", "FRACTALX_REGISTRY_URL","http://localhost:8761", "FRACTALX_LOGGER_URL","http://localhost:9099")),
        new ServiceConfig("recruitment-service", 8084, 18084, "com.example.hr.recruitment", "com.example.hr.recruitment.RecruitmentModule", true, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker", "OTEL_EXPORTER_OTLP_ENDPOINT","http://localhost:4317", "OTEL_SERVICE_NAME","recruitment-service", "FRACTALX_REGISTRY_URL","http://localhost:8761", "FRACTALX_LOGGER_URL","http://localhost:9099", "DEPARTMENT_SERVICE_HOST","department-service", "EMPLOYEE_SERVICE_HOST","employee-service")),
        new ServiceConfig("fractalx-registry", 8761, 0, "", "", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker","FRACTALX_REGISTRY_URL","http://localhost:8761")),
        new ServiceConfig("api-gateway",       9999, 0, "", "", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker")),
        new ServiceConfig("admin-service",     9090, 0, "", "", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker","FRACTALX_LOGGER_URL","http://localhost:9099")),
        new ServiceConfig("logger-service",    9099, 0, "", "", false, false, List.of(), Map.of("SPRING_PROFILES_ACTIVE","docker"))
    );

    public List<ServiceConfig> getAll()                       { return CONFIGS; }
    public int                 count()                        { return CONFIGS.size(); }

    public Optional<ServiceConfig> findByName(String name) {
        return CONFIGS.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    public List<ServiceConfig> getMicroservices() {
        return CONFIGS.stream()
                .filter(c -> c.httpPort() > 0 && c.grpcPort() > 0)
                .toList();
    }
}
