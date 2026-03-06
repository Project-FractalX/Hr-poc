package org.fractalx.generated.recruitmentservice;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers custom health indicators and Micrometer gauges for each
 * NetScope peer dependency of this service. Exposed via
 * {@code GET /actuator/health} (show-details: always).
 */
@Configuration
public class ServiceHealthConfig {
    
    /** Health indicator for the "department-service" NetScope peer (gRPC on port 18085). */
    @Bean
    public HealthIndicator departmentServiceHealthIndicator(
            @Value("${netscope.client.servers.department-service.host:localhost}") String host,
            @Value("${netscope.client.servers.department-service.port:18085}")        int    port) {
        return () -> {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress(host, port), 2_000);
                return Health.up()
                        .withDetail("service",  "department-service")
                        .withDetail("grpcPort", port)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "department-service")
                        .withDetail("error",   e.getMessage())
                        .build();
            }
        };
    }

    /** Health indicator for the "employee-service" NetScope peer (gRPC on port 18081). */
    @Bean
    public HealthIndicator employeeServiceHealthIndicator(
            @Value("${netscope.client.servers.employee-service.host:localhost}") String host,
            @Value("${netscope.client.servers.employee-service.port:18081}")        int    port) {
        return () -> {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress(host, port), 2_000);
                return Health.up()
                        .withDetail("service",  "employee-service")
                        .withDetail("grpcPort", port)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "employee-service")
                        .withDetail("error",   e.getMessage())
                        .build();
            }
        };
    }

    
    /** Micrometer gauge: fractalx.service.dependency.up{service="department-service"} */
    @Bean
    public MeterBinder departmentServiceDependencyGauge(
            @Qualifier("departmentServiceHealthIndicator") HealthIndicator departmentServiceHealthIndicator) {
        return registry -> Gauge.builder(
                "fractalx.service.dependency.up",
                departmentServiceHealthIndicator,
                hi -> "UP".equals(hi.health().getStatus().getCode()) ? 1.0 : 0.0)
                .tag("service", "department-service")
                .description("1 if the department-service dependency is reachable, 0 otherwise")
                .register(registry);
    }

    /** Micrometer gauge: fractalx.service.dependency.up{service="employee-service"} */
    @Bean
    public MeterBinder employeeServiceDependencyGauge(
            @Qualifier("employeeServiceHealthIndicator") HealthIndicator employeeServiceHealthIndicator) {
        return registry -> Gauge.builder(
                "fractalx.service.dependency.up",
                employeeServiceHealthIndicator,
                hi -> "UP".equals(hi.health().getStatus().getCode()) ? 1.0 : 0.0)
                .tag("service", "employee-service")
                .description("1 if the employee-service dependency is reachable, 0 otherwise")
                .register(registry);
    }

}
