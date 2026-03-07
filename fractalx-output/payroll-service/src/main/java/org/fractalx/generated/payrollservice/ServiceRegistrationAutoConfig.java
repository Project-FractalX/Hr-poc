package org.fractalx.generated.payrollservice;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "fractalx.registry.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceRegistrationAutoConfig {

    private final FractalRegistryClient registryClient;

    @Value("${spring.application.name:payroll-service}")
    private String serviceName;

    @Value("${fractalx.registry.host:localhost}")
    private String serviceHost;

    @Value("${server.port:8082}")
    private int httpPort;

    public ServiceRegistrationAutoConfig(FractalRegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @PostConstruct
    public void onStartup() {
        String healthUrl = "http://" + serviceHost + ":" + httpPort + "/actuator/health";
        registryClient.register(serviceName, serviceHost, httpPort, 18082, healthUrl);
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        registryClient.heartbeat(serviceName);
    }

    @PreDestroy
    public void onShutdown() {
        registryClient.deregister(serviceName);
    }
}
