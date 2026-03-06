package org.fractalx.generated.leaveservice;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetScopeResilienceConfig {
    
    @Bean
    public CircuitBreaker payrollServiceCb(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("payroll-service");
    }

    @Bean
    public CircuitBreaker employeeServiceCb(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("employee-service");
    }

}
