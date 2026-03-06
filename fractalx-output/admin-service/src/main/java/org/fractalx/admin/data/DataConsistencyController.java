package org.fractalx.admin.data;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST API for the Data Consistency section of the admin dashboard.
 *
 * <pre>
 * GET /api/data/overview              — summary of all data consistency features
 * GET /api/data/sagas                 — all baked saga definitions
 * GET /api/data/sagas/instances       — proxy to saga-orchestrator GET /saga
 * GET /api/data/sagas/{sagaId}/instances — instances filtered by sagaId
 * GET /api/data/databases             — per-service DB health (actuator/health/db)
 * GET /api/data/schemas               — per-service owned schemas
 * GET /api/data/outbox                — per-service outbox metrics
 * </pre>
 */
@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataConsistencyController {

    private static final int SAGA_ORCHESTRATOR_PORT = 8099;

    private final SagaMetaRegistry registry;
    private final RestTemplate     restTemplate = new RestTemplate();

    public DataConsistencyController(SagaMetaRegistry registry) {
        this.registry = registry;
    }

    /** Overview: service count, saga count, schema summary. */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalServices",  5);
        overview.put("totalSagas",     registry.count());
        overview.put("hasSagas",       registry.hasSagas());
        overview.put("sagaOrchestrator",
            Map.of("port", SAGA_ORCHESTRATOR_PORT,
                   "health", fetchSagaOrchestratorHealth()));
        return ResponseEntity.ok(overview);
    }

    /** Returns all baked saga definitions. */
    @GetMapping("/sagas")
    public ResponseEntity<List<SagaMetaRegistry.SagaInfo>> getSagas() {
        return ResponseEntity.ok(registry.getAll());
    }

    /** Proxies to saga-orchestrator GET /saga for all saga instances. */
    @GetMapping("/sagas/instances")
    public ResponseEntity<Object> getAllSagaInstances() {
        try {
            Object resp = restTemplate.getForObject(
                    "http://localhost:" + SAGA_ORCHESTRATOR_PORT + "/saga", Object.class);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "error", "Saga orchestrator unavailable: " + e.getMessage()));
        }
    }

    /** Proxies to saga-orchestrator filtered by sagaId. */
    @GetMapping("/sagas/{sagaId}/instances")
    public ResponseEntity<Object> getSagaInstances(@PathVariable("sagaId") String sagaId) {
        try {
            Object resp = restTemplate.getForObject(
                    "http://localhost:" + SAGA_ORCHESTRATOR_PORT + "/saga?sagaId=" + sagaId,
                    Object.class);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "error", "Saga orchestrator unavailable: " + e.getMessage()));
        }
    }

    /**
     * Returns saga instances enriched with per-step status derived from
     * the baked SagaMetaRegistry definitions.
     */
    @GetMapping("/sagas/instances/enriched")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> getEnrichedInstances() {
        try {
            List<Map<String, Object>> instances = (List<Map<String, Object>>)
                    restTemplate.getForObject(
                            "http://localhost:" + SAGA_ORCHESTRATOR_PORT + "/saga",
                            List.class);
            if (instances == null) return ResponseEntity.ok(new ArrayList<>());

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> inst : instances) {
                Map<String, Object> enriched = new LinkedHashMap<>(inst);
                String sagaId     = (String) inst.get("sagaId");
                String currentStep = (String) inst.get("currentStep");
                String sagaStatus  = String.valueOf(inst.get("status"));
                registry.findById(sagaId).ifPresent(def -> {
                    enriched.put("steps", def.steps());
                    enriched.put("compensationSteps", def.compensationSteps());
                    List<Map<String, Object>> progress = new ArrayList<>();
                    boolean[] found = {false};
                    for (String step : def.steps()) {
                        Map<String, Object> sp = new LinkedHashMap<>();
                        sp.put("step", step);
                        String st;
                        if ("DONE".equals(sagaStatus)) {
                            st = "COMPLETED";
                        } else if (step.equals(currentStep)) {
                            st = ("FAILED".equals(sagaStatus) || "COMPENSATING".equals(sagaStatus))
                                    ? "FAILED" : "IN_PROGRESS";
                            found[0] = true;
                        } else if (!found[0]) {
                            st = "COMPLETED";
                        } else {
                            st = "PENDING";
                        }
                        sp.put("status", st);
                        progress.add(sp);
                    }
                    enriched.put("stepProgress", progress);
                });
                result.add(enriched);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "error", "Saga orchestrator unavailable: " + e.getMessage()));
        }
    }

    /** Per-service database health from /actuator/health/db. */
    @GetMapping("/databases")
    public ResponseEntity<List<Map<String, Object>>> getDatabases() {
        List<Map<String, Object>> dbList = new ArrayList<>();
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","department-service"); db.put("schemas","default"); db.put("health", fetchDbHealth(8085)); dbList.add(db);}
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","employee-service"); db.put("schemas","default"); db.put("health", fetchDbHealth(8081)); dbList.add(db);}
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","leave-service"); db.put("schemas","default"); db.put("health", fetchDbHealth(8083)); dbList.add(db);}
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","payroll-service"); db.put("schemas","default"); db.put("health", fetchDbHealth(8082)); dbList.add(db);}
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","recruitment-service"); db.put("schemas","default"); db.put("health", fetchDbHealth(8084)); dbList.add(db);}
        {Map<String,Object> db = new LinkedHashMap<>(); db.put("service","saga-orchestrator"); db.put("schemas","saga_instance"); db.put("health", fetchDbHealth(SAGA_ORCHESTRATOR_PORT)); db.put("instanceCount", fetchSagaInstanceCount()); dbList.add(db);}
                return ResponseEntity.ok(dbList);
    }

    /** Per-service owned schema info (baked from generation metadata). */
    @GetMapping("/schemas")
    public ResponseEntity<List<Map<String, Object>>> getSchemas() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SagaMetaRegistry.SagaInfo saga : registry.getAll()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("service", saga.orchestratedBy());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /** Per-service outbox metrics from /actuator/metrics. */
    @GetMapping("/outbox")
    public ResponseEntity<List<Map<String, Object>>> getOutbox() {
        List<Map<String, Object>> outboxList = new ArrayList<>();
        {Map<String,Object> ob = new LinkedHashMap<>(); ob.put("service","department-service"); ob.put("metrics", fetchOutboxMetrics(8085)); outboxList.add(ob);}
        {Map<String,Object> ob = new LinkedHashMap<>(); ob.put("service","employee-service"); ob.put("metrics", fetchOutboxMetrics(8081)); outboxList.add(ob);}
        {Map<String,Object> ob = new LinkedHashMap<>(); ob.put("service","leave-service"); ob.put("metrics", fetchOutboxMetrics(8083)); outboxList.add(ob);}
        {Map<String,Object> ob = new LinkedHashMap<>(); ob.put("service","payroll-service"); ob.put("metrics", fetchOutboxMetrics(8082)); outboxList.add(ob);}
        {Map<String,Object> ob = new LinkedHashMap<>(); ob.put("service","recruitment-service"); ob.put("metrics", fetchOutboxMetrics(8084)); outboxList.add(ob);}
                return ResponseEntity.ok(outboxList);
    }

    // -------------------------------------------------------------------------

    private String fetchDbHealth(int port) {
        try {
            Object resp = restTemplate.getForObject(
                    "http://localhost:" + port + "/actuator/health/db", Object.class);
            return resp != null && resp.toString().contains("UP") ? "UP" : "DOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Object fetchOutboxMetrics(int port) {
        try {
            return restTemplate.getForObject(
                    "http://localhost:" + port + "/actuator/metrics", Object.class);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private String fetchSagaOrchestratorHealth() {
        try {
            String resp = restTemplate.getForObject(
                    "http://localhost:" + SAGA_ORCHESTRATOR_PORT + "/actuator/health",
                    String.class);
            return (resp != null && resp.contains("UP")) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    @SuppressWarnings("unchecked")
    private int fetchSagaInstanceCount() {
        try {
            List<?> resp = (List<?>) restTemplate.getForObject(
                    "http://localhost:" + SAGA_ORCHESTRATOR_PORT + "/saga", List.class);
            return resp != null ? resp.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }
}
