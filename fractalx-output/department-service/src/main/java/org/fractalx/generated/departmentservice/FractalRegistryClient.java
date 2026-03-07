package org.fractalx.generated.departmentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class FractalRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(FractalRegistryClient.class);

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void register(String name, String host, int port, int grpcPort, String healthUrl) {
        try {
            Map<String, Object> payload = Map.of(
                    "name", name,
                    "host", host,
                    "port", port,
                    "grpcPort", grpcPort,
                    "healthUrl", healthUrl
            );
            restTemplate.postForObject(registryUrl + "/services", payload, Object.class);
            log.info("Registered with fractalx-registry: {} at {}:{}", name, host, port);
        } catch (Exception e) {
            log.warn("Could not register with fractalx-registry ({}): {}", registryUrl, e.getMessage());
        }
    }

    public void deregister(String name) {
        try {
            restTemplate.delete(registryUrl + "/services/" + name + "/deregister");
            log.info("Deregistered from fractalx-registry: {}", name);
        } catch (Exception e) {
            log.warn("Could not deregister from fractalx-registry: {}", e.getMessage());
        }
    }

    public void heartbeat(String name) {
        try {
            restTemplate.postForObject(registryUrl + "/services/" + name + "/heartbeat",
                    null, Void.class);
        } catch (Exception e) {
            log.trace("Heartbeat failed for {}: {}", name, e.getMessage());
        }
    }
}
