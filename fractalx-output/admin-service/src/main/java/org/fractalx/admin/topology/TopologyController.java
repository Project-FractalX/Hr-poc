package org.fractalx.admin.topology;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TopologyController {

    private final ServiceTopologyProvider topologyProvider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Environment  environment;

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    public TopologyController(ServiceTopologyProvider topologyProvider, Environment environment) {
        this.topologyProvider = topologyProvider;
        this.environment      = environment;
    }

    /** Returns the static service dependency graph (nodes + edges). */
    @GetMapping("/topology")
    public ResponseEntity<TopologyGraph> getTopology() {
        return ResponseEntity.ok(topologyProvider.getTopology());
    }

    /** Polls live health from each known service and returns a name→status map. */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, String>> getHealthSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("department-service", checkHealth("department-service", 8085, "/actuator/health"));
        summary.put("employee-service", checkHealth("employee-service", 8081, "/actuator/health"));
        summary.put("leave-service", checkHealth("leave-service", 8083, "/actuator/health"));
        summary.put("payroll-service", checkHealth("payroll-service", 8082, "/actuator/health"));
        summary.put("recruitment-service", checkHealth("recruitment-service", 8084, "/actuator/health"));
        summary.put("fractalx-registry", checkHealth("fractalx-registry", 8761, "/services/health"));
        summary.put("fractalx-gateway",  checkHealth("fractalx-gateway",  9999, "/actuator/health"));
        summary.put("admin-service",     "UP"); // self — always reachable if this endpoint is responding
        summary.put("logger-service",    checkHealth("logger-service", 9099, "/actuator/health"));

        return ResponseEntity.ok(summary);
    }

    /** Proxies the fractalx-registry /services endpoint for the admin UI. */
    @GetMapping("/services")
    public ResponseEntity<Object> getLiveServices() {
        try {
            return ResponseEntity.ok(
                    restTemplate.getForObject(registryUrl + "/services", Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(
                    Map.of("error", "Registry unavailable: " + e.getMessage()));
        }
    }

    /**
     * TCP-first check: RUNNING if the port accepts a connection,
     * then optionally confirm via actuator path.
     */
    private String checkHealth(String serviceName, int port, String actuatorPath) {
        boolean docker = Arrays.asList(environment.getActiveProfiles()).contains("docker");
        String host = docker ? serviceName : "localhost";
        // Phase 1: is the process up at all?
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 2000);
        } catch (Exception e) {
            return "DOWN";
        }
        // Phase 2: actuator health
        try {
            String resp = restTemplate.getForObject(
                    "http://" + host + ":" + port + actuatorPath, String.class);
            if (resp == null) return "RUNNING";
            boolean anyDown = resp.contains("DOWN");
            boolean allUp   = resp.contains("UP") && !anyDown;
            return allUp ? "UP" : anyDown ? "DEGRADED" : "RUNNING";
        } catch (Exception e) {
            return "RUNNING"; // port open but actuator not exposed
        }
    }
}
