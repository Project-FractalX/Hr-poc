package org.fractalx.admin.topology;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TopologyController {

    private final ServiceTopologyProvider topologyProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    public TopologyController(ServiceTopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
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
        summary.put("department-service", checkHealth("http://localhost:8085/actuator/health"));
        summary.put("employee-service", checkHealth("http://localhost:8081/actuator/health"));
        summary.put("leave-service", checkHealth("http://localhost:8083/actuator/health"));
        summary.put("payroll-service", checkHealth("http://localhost:8082/actuator/health"));
        summary.put("recruitment-service", checkHealth("http://localhost:8084/actuator/health"));
        summary.put("fractalx-registry", checkHealth("http://localhost:8761/services/health"));
        summary.put("fractalx-gateway",  checkHealth("http://localhost:9999/actuator/health"));

        return ResponseEntity.ok(summary);
    }

    /** Proxies the fractalx-registry /services endpoint for the admin UI. */
    @GetMapping("/services")
    public ResponseEntity<Object> getLiveServices() {
        try {
            String registryUrl = System.getProperty("fractalx.registry.url",
                    "http://localhost:8761");
            return ResponseEntity.ok(
                    restTemplate.getForObject(registryUrl + "/services", Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(
                    Map.of("error", "Registry unavailable: " + e.getMessage()));
        }
    }

    private String checkHealth(String healthUrl) {
        try {
            String resp = restTemplate.getForObject(healthUrl, String.class);
            return (resp != null && resp.contains("UP")) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
