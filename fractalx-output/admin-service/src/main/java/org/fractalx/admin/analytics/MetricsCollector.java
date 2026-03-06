package org.fractalx.admin.analytics;

import org.fractalx.admin.services.ServiceMetaRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls each service's /actuator/metrics endpoints every 15 seconds and records
 * snapshots in {@link MetricsHistoryStore}.
 */
@Component
public class MetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);
    private static final int    INTERVAL_S = 15;

    private final MetricsHistoryStore                    store;
    private final ServiceMetaRegistry                    registry;
    private final RestTemplate                           rest;
    private final ConcurrentHashMap<String, double[]>    prevCounts = new ConcurrentHashMap<>();

    public MetricsCollector(MetricsHistoryStore store, ServiceMetaRegistry registry) {
        this.store    = store;
        this.registry = registry;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(3_000);
        this.rest = new RestTemplate(factory);
    }

    @Scheduled(fixedDelay = INTERVAL_S * 1000L)
    public void collect() {
        for (ServiceMetaRegistry.ServiceMeta svc : registry.getAll()) {
            if (svc.port() <= 0) continue;
            try {
                String base = "http://localhost:" + svc.port();
                store.record(svc.name(), collectService(svc.name(), base));
            } catch (Exception e) {
                log.debug("Metrics unavailable for {}: {}", svc.name(), e.getMessage());
            }
        }
    }

    private MetricsHistoryStore.MetricsSnapshot collectService(String name, String base) {
        double cpu      = fetchGauge(base, "process.cpu.usage") * 100.0;
        double heapUsed = fetchGauge(base, "jvm.memory.used?tag=area:heap") / (1024.0 * 1024.0);
        double heapMax  = fetchGauge(base, "jvm.memory.max?tag=area:heap")  / (1024.0 * 1024.0);
        long   threads  = (long) fetchGauge(base, "jvm.threads.live");
        double p99s     = fetchStat(base, "http.server.requests", "MAX");
        double totalReq = fetchStat(base, "http.server.requests", "COUNT");
        double errReq   = fetchStat(base, "http.server.requests?tag=status:5xx", "COUNT");
        double[] prev   = prevCounts.getOrDefault(name, new double[]{totalReq, errReq});
        double dTotal   = Math.max(0, totalReq - prev[0]);
        double dErr     = Math.max(0, errReq   - prev[1]);
        prevCounts.put(name, new double[]{totalReq, errReq});
        double rps       = dTotal / INTERVAL_S;
        double errorRate = dTotal > 0 ? dErr / dTotal * 100.0 : 0.0;
        return new MetricsHistoryStore.MetricsSnapshot(
                Instant.now(), cpu, heapUsed, heapMax, threads, rps, errorRate, p99s * 1000.0);
    }

    @SuppressWarnings("unchecked")
    private double fetchGauge(String base, String path) {
        try {
            Map<String, Object> body = rest.getForObject(base + "/actuator/metrics/" + path, Map.class);
            if (body == null) return 0.0;
            List<Map<String, Object>> m = (List<Map<String, Object>>) body.get("measurements");
            if (m == null || m.isEmpty()) return 0.0;
            Object v = m.get(0).get("value");
            return v instanceof Number n ? n.doubleValue() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    @SuppressWarnings("unchecked")
    private double fetchStat(String base, String path, String statistic) {
        try {
            Map<String, Object> body = rest.getForObject(base + "/actuator/metrics/" + path, Map.class);
            if (body == null) return 0.0;
            List<Map<String, Object>> ms = (List<Map<String, Object>>) body.get("measurements");
            if (ms == null) return 0.0;
            for (Map<String, Object> m : ms)
                if (statistic.equals(m.get("statistic"))) {
                    Object v = m.get("value");
                    return v instanceof Number n ? n.doubleValue() : 0.0;
                }
            return 0.0;
        } catch (Exception e) { return 0.0; }
    }
}
