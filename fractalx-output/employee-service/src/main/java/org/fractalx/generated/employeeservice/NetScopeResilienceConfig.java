package org.fractalx.generated.employeeservice;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetScopeResilienceConfig {
    
    @Bean
    public CircuitBreaker departmentServiceCb(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("department-service");
    }

    @Bean
    public CircuitBreaker payrollServiceCb(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("payroll-service");
    }

}
