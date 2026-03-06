package org.fractalx.admin.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for the admin observability and alerting subsystem.
 *
 * <pre>
 * GET  /api/observability/metrics       — per-service health + latency snapshot
 * GET  /api/traces                      — proxy to Jaeger query API
 * GET  /api/traces/{traceId}            — single trace from Jaeger
 * GET  /api/alerts                      — paginated alert history
 * GET  /api/alerts/active               — unresolved alerts only
 * POST /api/alerts/{id}/resolve         — manually resolve an alert
 * GET  /api/alerts/stream               — SSE real-time alert feed
 * GET  /api/alerts/config               — current alert rule configuration
 * PUT  /api/alerts/config/rules         — update alert rules
 * GET  /api/logs                        — proxy to logger-service
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class ObservabilityController {

    private final AlertStore              alertStore;
    private final AlertConfigProperties   alertConfig;
    private final AlertChannels           alertChannels;
    private final RestTemplate            rest = new RestTemplate();

    @Value("${fractalx.observability.jaeger.query-url:http://localhost:16686}")
    private String jaegerQueryUrl;

    @Value("${fractalx.observability.logger-url:http://localhost:9099}")
    private String loggerUrl;

    public ObservabilityController(AlertStore store,
                                   AlertConfigProperties config,
                                   AlertChannels channels) {
        this.alertStore    = store;
        this.alertConfig   = config;
        this.alertChannels = channels;
    }

    // ---- Metrics ----

    @GetMapping("/observability/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("department-service", fetchServiceMetrics("department-service", "http://localhost:8085/actuator/health",
            "http://localhost:8085/actuator/metrics/http.server.requests"));
    metrics.put("employee-service", fetchServiceMetrics("employee-service", "http://localhost:8081/actuator/health",
            "http://localhost:8081/actuator/metrics/http.server.requests"));
    metrics.put("leave-service", fetchServiceMetrics("leave-service", "http://localhost:8083/actuator/health",
            "http://localhost:8083/actuator/metrics/http.server.requests"));
    metrics.put("payroll-service", fetchServiceMetrics("payroll-service", "http://localhost:8082/actuator/health",
            "http://localhost:8082/actuator/metrics/http.server.requests"));
    metrics.put("recruitment-service", fetchServiceMetrics("recruitment-service", "http://localhost:8084/actuator/health",
            "http://localhost:8084/actuator/metrics/http.server.requests"));

        return ResponseEntity.ok(metrics);
    }

    // ---- Traces (Jaeger proxy) ----

    @GetMapping("/traces")
    public ResponseEntity<Object> getTraces(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            // If correlationId given but no service, search across ALL Jaeger services.
            // Jaeger's /api/traces requires a service parameter, so we fetch the service
            // list first, query each one, then merge and return deduped results.
            if (correlationId != null && !correlationId.isBlank()
                    && (service == null || service.isBlank())) {
                return searchByCorrelationAcrossAllServices(correlationId, limit);
            }
            StringBuilder url = new StringBuilder(jaegerQueryUrl + "/api/traces?limit=" + limit);
            if (service != null && !service.isBlank()) url.append("&service=").append(service);
            if (correlationId != null && !correlationId.isBlank()) {
                String tagsJson = "{\"correlation.id\":\"" + correlationId.replace("\"", "") + "\"}";
                url.append("&tags=").append(java.net.URLEncoder.encode(tagsJson, java.nio.charset.StandardCharsets.UTF_8));
            }
            return ResponseEntity.ok(rest.getForObject(url.toString(), Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Jaeger unavailable: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Object> searchByCorrelationAcrossAllServices(String correlationId, int limit) {
        try {
            // 1. Fetch list of services that have reported spans to Jaeger
            Map<String, Object> svcResp = rest.getForObject(jaegerQueryUrl + "/api/services", Map.class);
            List<String> services = svcResp != null && svcResp.get("data") instanceof List<?>
                    ? (List<String>) svcResp.get("data") : List.of();

            // 2. Query each service for traces with this correlation ID
            List<Object> merged = new java.util.ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String svc : services) {
                try {
                    String tagsJson = "{\"correlation.id\":\"" + correlationId.replace("\"", "") + "\"}";
                    String url = jaegerQueryUrl + "/api/traces?service=" + svc
                            + "&tags=" + java.net.URLEncoder.encode(tagsJson, java.nio.charset.StandardCharsets.UTF_8)
                            + "&limit=" + limit;
                    Map<String, Object> result = rest.getForObject(url, Map.class);
                    if (result != null && result.get("data") instanceof List<?> data) {
                        for (Object trace : data) {
                            if (trace instanceof Map<?, ?> t) {
                                String tid = String.valueOf(t.get("traceID"));
                                if (seen.add(tid)) merged.add(trace);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            return ResponseEntity.ok(Map.of("data", merged));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Jaeger service list unavailable: " + e.getMessage()));
        }
    }

    @GetMapping("/traces/services")
    public ResponseEntity<Object> getJaegerServices() {
        try {
            return ResponseEntity.ok(rest.getForObject(jaegerQueryUrl + "/api/services", Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<Object> getTrace(@PathVariable String traceId) {
        try {
            return ResponseEntity.ok(
                    rest.getForObject(jaegerQueryUrl + "/api/traces/" + traceId, Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Jaeger unavailable: " + e.getMessage()));
        }
    }

    // ---- Alerts ----

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertEvent>> getAlerts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertStore.findAll(page, size));
    }

    @GetMapping("/alerts/active")
    public ResponseEntity<List<AlertEvent>> getActiveAlerts() {
        return ResponseEntity.ok(alertStore.findUnresolved());
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String id) {
        boolean resolved = alertStore.resolve(id);
        return ResponseEntity.ok(Map.of("resolved", resolved, "id", id));
    }

    @GetMapping("/alerts/stream")
    public SseEmitter streamAlerts() {
        return alertChannels.subscribeAdminUi();
    }

    @GetMapping("/alerts/config")
    public ResponseEntity<AlertConfigProperties> getAlertConfig() {
        return ResponseEntity.ok(alertConfig);
    }

    @PutMapping("/alerts/config/rules")
    public ResponseEntity<List<AlertRule>> updateRules(@RequestBody List<AlertRule> rules) {
        alertConfig.setRules(rules);
        return ResponseEntity.ok(alertConfig.getRules());
    }

    // ---- Logs (logger-service proxy) ----

    @GetMapping("/logs")
    public ResponseEntity<Object> getLogs(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            StringBuilder url = new StringBuilder(loggerUrl + "/api/logs?page=" + page + "&size=" + size);
            if (correlationId != null) url.append("&correlationId=").append(correlationId);
            if (service != null)       url.append("&service=").append(service);
            if (level != null)         url.append("&level=").append(level);
            return ResponseEntity.ok(rest.getForObject(url.toString(), Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", "Logger service unavailable: " + e.getMessage()));
        }
    }

    @GetMapping("/logs/services")
    public ResponseEntity<Object> getLogServices() {
        try {
            return ResponseEntity.ok(rest.getForObject(loggerUrl + "/api/logs/services", Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/logs/stats")
    public ResponseEntity<Object> getLogStats() {
        try {
            return ResponseEntity.ok(rest.getForObject(loggerUrl + "/api/logs/stats", Object.class));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of());
        }
    }

    // ---- Helper ----

    private Map<String, Object> fetchServiceMetrics(String service, String healthUrl, String metricsUrl) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("service", service);
        try {
            String health = rest.getForObject(healthUrl, String.class);
            snap.put("health", health != null && health.contains("\"UP\"") ? "UP" : "DOWN");
        } catch (Exception e) {
            snap.put("health", "DOWN");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = rest.getForObject(metricsUrl, Map.class);
            if (m != null) snap.put("metrics", m);
        } catch (Exception e) {
            snap.put("metrics", Map.of());
        }
        return snap;
    }
}
