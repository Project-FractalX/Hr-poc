package org.fractalx.admin.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST API for the Services section of the admin dashboard.
 *
 * <pre>
 * GET /api/services/all              — all services with live health + commands
 * GET /api/services/{name}/detail    — full detail (health, metrics, commands)
 * GET /api/services/{name}/health    — proxy to actuator health
 * GET /api/services/{name}/metrics   — proxy to actuator metrics
 * GET /api/services/{name}/deployment — deployment stages
 * GET /api/services/{name}/history   — full deployment history
 * GET /api/services/{name}/commands  — docker-compose lifecycle commands
 * </pre>
 */
@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServicesController {

    private final ServiceMetaRegistry    registry;
    private final DeploymentTracker      deploymentTracker;
    private final RestTemplate           restTemplate = new RestTemplate();

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    public ServicesController(ServiceMetaRegistry registry, DeploymentTracker deploymentTracker) {
        this.registry          = registry;
        this.deploymentTracker = deploymentTracker;
    }

    /** Returns all services with live health status, metadata, and lifecycle commands. */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllServices() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceMetaRegistry.ServiceMeta meta : registry.getAll()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("meta",       meta);
            entry.put("health",     fetchHealthStatus(meta));
            entry.put("commands",   buildCommands(meta.name()));
            entry.put("deployment", deploymentTracker.getLatest(meta.name()));
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /** Full detail for one service: health, metrics snapshot, dependencies, commands. */
    @GetMapping("/{name}/detail")
    public ResponseEntity<Map<String, Object>> getServiceDetail(@PathVariable("name") String name) {
        return registry.findByName(name).map(meta -> {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("meta",       meta);
            detail.put("health",     fetchHealthFull(meta));
            detail.put("commands",   buildCommands(name));
            detail.put("deployment", deploymentTracker.getLatest(name));
            return ResponseEntity.ok(detail);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Proxies to the service's /actuator/health endpoint. */
    @GetMapping("/{name}/health")
    public ResponseEntity<Object> getServiceHealth(@PathVariable("name") String name) {
        return registry.findByName(name).map(meta -> {
            if (meta.port() == 0) return ResponseEntity.ok((Object) Map.of("status", "UNKNOWN"));
            try {
                Object resp = restTemplate.getForObject(
                        "http://localhost:" + meta.port() + "/actuator/health", Object.class);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                return ResponseEntity.ok((Object) Map.of("status", "DOWN", "error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Proxies to the service's /actuator/metrics endpoint. */
    @GetMapping("/{name}/metrics")
    public ResponseEntity<Object> getServiceMetrics(@PathVariable("name") String name) {
        return registry.findByName(name).map(meta -> {
            if (meta.port() == 0)
                return ResponseEntity.ok((Object) Map.of("error", "No metrics port configured"));
            try {
                Object resp = restTemplate.getForObject(
                        "http://localhost:" + meta.port() + "/actuator/metrics", Object.class);
                return ResponseEntity.ok(resp);
            } catch (Exception e) {
                return ResponseEntity.ok((Object) Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Returns the latest deployment record + stages for a service. */
    @GetMapping("/{name}/deployment")
    public ResponseEntity<Object> getDeployment(@PathVariable("name") String name) {
        DeploymentTracker.DeploymentRecord record = deploymentTracker.getLatest(name);
        if (record == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(record);
    }

    /** Returns the full deployment history for a service. */
    @GetMapping("/{name}/history")
    public ResponseEntity<List<DeploymentTracker.DeploymentRecord>> getHistory(
            @PathVariable("name") String name) {
        return ResponseEntity.ok(deploymentTracker.getHistory(name));
    }

    /** Returns docker-compose lifecycle commands (start/stop/restart/logs). */
    @GetMapping("/{name}/commands")
    public ResponseEntity<Map<String, String>> getLifecycleCommands(@PathVariable("name") String name) {
        return ResponseEntity.ok(buildCommands(name));
    }

    // -------------------------------------------------------------------------

    private String fetchHealthStatus(ServiceMetaRegistry.ServiceMeta meta) {
        if (meta.port() == 0) return "UNKNOWN";
        try {
            String resp = restTemplate.getForObject(
                    "http://localhost:" + meta.port() + "/actuator/health", String.class);
            return (resp != null && resp.contains("UP")) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private Object fetchHealthFull(ServiceMetaRegistry.ServiceMeta meta) {
        if (meta.port() == 0) return Map.of("status", "UNKNOWN");
        try {
            return restTemplate.getForObject(
                    "http://localhost:" + meta.port() + "/actuator/health", Object.class);
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> buildCommands(String service) {
        Map<String, String> cmds = new LinkedHashMap<>();
        cmds.put("start",   "docker compose up -d " + service);
        cmds.put("stop",    "docker compose stop " + service);
        cmds.put("restart", "docker compose restart " + service);
        cmds.put("logs",    "docker compose logs -f " + service);
        return cmds;
    }
}
