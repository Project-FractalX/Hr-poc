package org.fractalx.admin.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically polls every generated service and evaluates alert rules.
 * Fires {@link AlertEvent}s via {@link NotificationDispatcher} on threshold breach.
 * Auto-resolves alerts when the service recovers.
 */
@Component
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertConfigProperties   config;
    private final AlertStore              store;
    private final NotificationDispatcher  dispatcher;
    private final RestTemplate            rest = new RestTemplate();

    // consecutive failure counter per "service::rule"
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();

    public AlertEvaluator(AlertConfigProperties config,
                          AlertStore store,
                          NotificationDispatcher dispatcher) {
        this.config     = config;
        this.store      = store;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${fractalx.alerting.eval-interval-ms:30000}")
    public void evaluate() {
        if (!config.isEnabled()) return;
        log.debug("Running alert evaluation cycle");
            evaluate("department-service",
             System.getenv("DEPARTMENT_SERVICE_BASE_URL") != null ? System.getenv("DEPARTMENT_SERVICE_BASE_URL") + "/actuator/health" : "http://localhost:8085/actuator/health",
             System.getenv("DEPARTMENT_SERVICE_BASE_URL") != null ? System.getenv("DEPARTMENT_SERVICE_BASE_URL") + "/actuator/metrics/http.server.requests" : "http://localhost:8085/actuator/metrics/http.server.requests");
    evaluate("employee-service",
             System.getenv("EMPLOYEE_SERVICE_BASE_URL") != null ? System.getenv("EMPLOYEE_SERVICE_BASE_URL") + "/actuator/health" : "http://localhost:8081/actuator/health",
             System.getenv("EMPLOYEE_SERVICE_BASE_URL") != null ? System.getenv("EMPLOYEE_SERVICE_BASE_URL") + "/actuator/metrics/http.server.requests" : "http://localhost:8081/actuator/metrics/http.server.requests");
    evaluate("leave-service",
             System.getenv("LEAVE_SERVICE_BASE_URL") != null ? System.getenv("LEAVE_SERVICE_BASE_URL") + "/actuator/health" : "http://localhost:8083/actuator/health",
             System.getenv("LEAVE_SERVICE_BASE_URL") != null ? System.getenv("LEAVE_SERVICE_BASE_URL") + "/actuator/metrics/http.server.requests" : "http://localhost:8083/actuator/metrics/http.server.requests");
    evaluate("payroll-service",
             System.getenv("PAYROLL_SERVICE_BASE_URL") != null ? System.getenv("PAYROLL_SERVICE_BASE_URL") + "/actuator/health" : "http://localhost:8082/actuator/health",
             System.getenv("PAYROLL_SERVICE_BASE_URL") != null ? System.getenv("PAYROLL_SERVICE_BASE_URL") + "/actuator/metrics/http.server.requests" : "http://localhost:8082/actuator/metrics/http.server.requests");
    evaluate("recruitment-service",
             System.getenv("RECRUITMENT_SERVICE_BASE_URL") != null ? System.getenv("RECRUITMENT_SERVICE_BASE_URL") + "/actuator/health" : "http://localhost:8084/actuator/health",
             System.getenv("RECRUITMENT_SERVICE_BASE_URL") != null ? System.getenv("RECRUITMENT_SERVICE_BASE_URL") + "/actuator/metrics/http.server.requests" : "http://localhost:8084/actuator/metrics/http.server.requests");

    }

    private void evaluate(String service, String healthUrl, String metricsUrl) {
        // --- health check ---
        try {
            String body = rest.getForObject(healthUrl, String.class);
            boolean up = body != null && body.contains("\"UP\"");
            processRule(service, "service-down", !up,
                    service + " health check failed — status not UP");
        } catch (Exception e) {
            processRule(service, "service-down", true,
                    service + " unreachable: " + e.getMessage());
        }

        // --- response-time check (Actuator metrics) ---
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = rest.getForObject(metricsUrl, Map.class);
            if (metrics != null) {
                double ms = extractDuration(metrics);
                for (AlertRule rule : config.getRules()) {
                    if ("response-time".equals(rule.getCondition()) && rule.isEnabled()) {
                        processRule(service, rule.getName(), ms > rule.getThreshold(),
                                service + " avg response time " + (int) ms + "ms > " + (int) rule.getThreshold() + "ms");
                    }
                }
            }
        } catch (Exception ignored) { /* metrics endpoint may not exist */ }
    }

    private void processRule(String service, String ruleName,
                             boolean breached, String message) {
        String key = service + "::" + ruleName;
        if (breached) {
            int count = failureCounts.merge(key, 1, Integer::sum);
            AlertRule rule = config.getRules().stream()
                    .filter(r -> ruleName.equals(r.getName()))
                    .findFirst().orElse(null);
            int threshold = rule != null ? rule.getConsecutiveFailures() : 1;
            if (count >= threshold && store.findUnresolved().stream()
                    .noneMatch(e -> service.equals(e.getService())
                            && ruleName.equals(e.getRule() != null ? e.getRule().getName() : ""))) {
                AlertEvent event = new AlertEvent();
                event.setService(service);
                event.setRule(rule);
                event.setSeverity(rule != null ? rule.getSeverity() : AlertSeverity.WARNING);
                event.setMessage(message);
                store.save(event);
                dispatcher.dispatch(event);
                log.warn("Alert fired: {} — {}", ruleName, message);
            }
        } else {
            failureCounts.put(key, 0);
            // auto-resolve matching open alerts
            store.findUnresolved().stream()
                    .filter(e -> service.equals(e.getService())
                            && ruleName.equals(e.getRule() != null ? e.getRule().getName() : ""))
                    .forEach(e -> {
                        store.resolve(e.getId());
                        log.info("Alert auto-resolved: {} for {}", ruleName, service);
                    });
        }
    }

    private double extractDuration(@SuppressWarnings("unchecked") Map<String, Object> metrics) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> measurements =
                    (java.util.List<Map<String, Object>>) metrics.get("measurements");
            if (measurements != null) {
                return measurements.stream()
                        .filter(m -> "TOTAL_TIME".equals(m.get("statistic")))
                        .mapToDouble(m -> ((Number) m.get("value")).doubleValue() * 1000)
                        .findFirst().orElse(0.0);
            }
        } catch (Exception ignored) { }
        return 0.0;
    }
}
