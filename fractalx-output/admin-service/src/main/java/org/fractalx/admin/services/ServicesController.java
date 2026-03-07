package org.fractalx.admin.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.Set;

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
    private final Environment            environment;

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    public ServicesController(ServiceMetaRegistry registry, DeploymentTracker deploymentTracker,
                              Environment environment) {
        this.registry          = registry;
        this.deploymentTracker = deploymentTracker;
        this.environment       = environment;
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

    /**
     * Returns a parsed health summary for a single service.
     * Distinguishes between the service itself being down vs a dependency being unavailable.
     */
    @GetMapping("/{name}/health-summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary(@PathVariable("name") String name) {
        return registry.findByName(name)
                .map(meta -> ResponseEntity.ok(fetchHealthStatus(meta)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Full detail for one service: structured health breakdown, dependencies, commands. */
    @GetMapping("/{name}/detail")
    public ResponseEntity<Map<String, Object>> getServiceDetail(@PathVariable("name") String name) {
        return registry.findByName(name).map(meta -> {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("meta",       meta);
            detail.put("health",     fetchHealthStatus(meta));
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
                        serviceBaseUrl(meta) + "/actuator/health", Object.class);
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
                        serviceBaseUrl(meta) + "/actuator/metrics", Object.class);
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

    /**
     * Two-phase health check:
     *
     * Phase 1 — TCP socket connect (2 s timeout).
     *   Like `nc -zv host port` — tells us whether the process is running,
     *   independent of any application-level health state.
     *   processStatus = RUNNING | UNREACHABLE
     *
     * Phase 2 — Actuator /health (only when RUNNING).
     *   Gives detailed component breakdown: DB, disk, dependencies, etc.
     *   status = UP | DEGRADED | DOWN
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchHealthStatus(ServiceMetaRegistry.ServiceMeta meta) {
        if (meta.port() == 0) {
            return buildResult("UNREACHABLE", "UNKNOWN", false, Map.of(), null);
        }

        // Phase 1: raw TCP — is the port open?
        String processStatus = tcpCheck(serviceHost(meta), meta.port());

        if ("UNREACHABLE".equals(processStatus)) {
            return buildResult("UNREACHABLE", "DOWN", false, Map.of(),
                    "Port " + meta.port() + " is not accepting connections");
        }

        // Phase 2: actuator health — application-level details
        try {
            Map<String, Object> raw = restTemplate.getForObject(
                    serviceBaseUrl(meta) + "/actuator/health", Map.class);
            if (raw == null) {
                return buildResult("RUNNING", "UNKNOWN", false, Map.of(), null);
            }
            Map<String, Object> result = classifyHealth(raw);
            result.put("processStatus", "RUNNING");
            return result;
        } catch (Exception e) {
            // Port is open but actuator is not exposed — treat as healthy process
            return buildResult("RUNNING", "UNKNOWN", false, Map.of(),
                    "Actuator not accessible: " +
                    (e.getMessage() != null ? e.getMessage() : "no response"));
        }
    }

    /** TCP socket connect with 2-second timeout — does not send any data. */
    private String tcpCheck(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return "RUNNING";
        } catch (Exception e) {
            return "UNREACHABLE";
        }
    }

    private String serviceHost(ServiceMetaRegistry.ServiceMeta meta) {
        boolean docker = Arrays.asList(environment.getActiveProfiles()).contains("docker");
        return docker ? meta.name() : "localhost";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> classifyHealth(Map<String, Object> raw) {
        String overall = String.valueOf(raw.getOrDefault("status", "DOWN")).toUpperCase();
        Map<String, Object> rawComps = raw.containsKey("components")
                ? (Map<String, Object>) raw.get("components") : Map.of();

        // Process indicators — these tell us the JVM/Spring context is alive
        Set<String> processKeys  = Set.of("ping", "livenessState", "readinessState");
        // Resource indicators — own infrastructure (disk, DB, refresh scope)
        Set<String> resourceKeys = Set.of("diskSpace", "refreshScope", "db",
                "mongo", "redis", "elasticsearch", "cassandra", "solr", "neo4j");

        // selfStatus: DOWN only if a process-level indicator is explicitly DOWN
        String selfStatus = "UP";
        for (String key : processKeys) {
            Object comp = rawComps.get(key);
            if (comp instanceof Map<?,?> cm) {
                Object sv = cm.get("status");
                String s = sv != null ? String.valueOf(sv).toUpperCase() : "UP";
                if ("DOWN".equals(s) || "OUT_OF_SERVICE".equals(s)) {
                    selfStatus = "DOWN"; break;
                }
            }
        }
        // If we got a response but have no components at all, service is reachable
        if (rawComps.isEmpty()) selfStatus = "UP".equals(overall) ? "UP" : "DOWN";

        boolean degraded = "UP".equals(selfStatus) && !"UP".equals(overall);
        String displayStatus = degraded ? "DEGRADED" : overall;

        // Build enriched component map: name → {status, category, error?, description?}
        Map<String, Map<String, String>> compMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawComps.entrySet()) {
            String name = entry.getKey();
            if (!(entry.getValue() instanceof Map<?,?> cm)) continue;

            Object sv = cm.get("status");
            String st = sv != null ? String.valueOf(sv).toUpperCase() : "UNKNOWN";

            String category = processKeys.contains(name) ? "process"
                    : resourceKeys.contains(name) ? "resource" : "dependency";

            Map<String, String> detail = new LinkedHashMap<>();
            detail.put("status",   st);
            detail.put("category", category);

            // Extract error message from details
            Object details = cm.get("details");
            if (details instanceof Map<?,?> dm) {
                Object err  = dm.get("error");
                Object desc = dm.get("description");
                if (err  != null) detail.put("error",       String.valueOf(err));
                if (desc != null) detail.put("description", String.valueOf(desc));
            }
            // description at component level
            Object compDesc = cm.get("description");
            if (compDesc != null && !detail.containsKey("description")) {
                detail.put("description", String.valueOf(compDesc));
            }
            compMap.put(name, detail);
        }

        return buildResult("RUNNING", displayStatus, degraded, compMap, null);
    }

    private Map<String, Object> buildResult(String processStatus, String status,
                                             boolean degraded,
                                             Map<String, ?> components, String error) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("processStatus", processStatus);
        r.put("status",        status);
        r.put("degraded",      degraded);
        r.put("components",    components);
        if (error != null) r.put("error", error);
        return r;
    }

    private String serviceBaseUrl(ServiceMetaRegistry.ServiceMeta meta) {
        return "http://" + serviceHost(meta) + ":" + meta.port();
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
