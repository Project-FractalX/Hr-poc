package org.fractalx.gateway.resilience;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayResilienceConfig {
    
    @Bean
    public ReactiveCircuitBreaker departmentServiceCb(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("department-service");
    }

    @Bean
    public ReactiveCircuitBreaker employeeServiceCb(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("employee-service");
    }

    @Bean
    public ReactiveCircuitBreaker leaveServiceCb(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("leave-service");
    }

    @Bean
    public ReactiveCircuitBreaker payrollServiceCb(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("payroll-service");
    }

    @Bean
    public ReactiveCircuitBreaker recruitmentServiceCb(ReactiveCircuitBreakerFactory<?, ?> factory) {
        return factory.create("recruitment-service");
    }

}
