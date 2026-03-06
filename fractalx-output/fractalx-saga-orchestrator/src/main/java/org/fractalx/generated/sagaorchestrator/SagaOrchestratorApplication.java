package org.fractalx.generated.sagaorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.fractalx.netscope.client.annotation.EnableNetScopeClient;

/**
 * FractalX Saga Orchestrator — Auto-Generated.
 * Coordinates distributed sagas across decomposed microservices.
 *
 * <p>{@code org.fractalx.runtime} is included in the component scan so that
 * FractalX runtime beans ({@code TraceFilter}, {@code NetScopeGrpcInterceptorConfigurer},
 * {@code NetScopeContextInterceptor}) are registered and wire the correlation ID
 * into outbound gRPC metadata automatically.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"org.fractalx.generated.sagaorchestrator", "org.fractalx.runtime"})
@EnableNetScopeClient(basePackages = {"org.fractalx.generated.sagaorchestrator.client"})
public class SagaOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SagaOrchestratorApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
