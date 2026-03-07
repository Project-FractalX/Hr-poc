package org.fractalx.admin.analytics;

import org.fractalx.admin.services.ServiceMetaRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Analytics REST API for the admin dashboard.
 *
 * <ul>
 *   <li>GET /api/analytics/overview          — aggregate snapshot (rps, cpu, errors, p99)</li>
 *   <li>GET /api/analytics/realtime          — per-service current metrics</li>
 *   <li>GET /api/analytics/history/{service} — 60-point time-series for one service</li>
 *   <li>GET /api/analytics/trends            — multi-service RPS + CPU time-series</li>
 *   <li>GET /api/analytics/stream            — SSE: live realtime snapshot every 5 s</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private final MetricsHistoryStore      store;
    private final ServiceMetaRegistry      registry;
    private final List<SseEmitter>         emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService pusher   =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "analytics-sse-pusher");
                t.setDaemon(true);
                return t;
            });

    public AnalyticsController(MetricsHistoryStore store, ServiceMetaRegistry registry) {
        this.store    = store;
        this.registry = registry;
        pusher.scheduleAtFixedRate(this::pushLive, 5, 5, TimeUnit.SECONDS);
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, MetricsHistoryStore.MetricsSnapshot> latest = store.getLatestAll();
        double totalRps = latest.values().stream().mapToDouble(MetricsHistoryStore.MetricsSnapshot::rps).sum();
        double avgCpu   = latest.values().stream().mapToDouble(MetricsHistoryStore.MetricsSnapshot::cpuPct).average().orElse(0);
        double avgErr   = latest.values().stream().mapToDouble(MetricsHistoryStore.MetricsSnapshot::errorRatePct).average().orElse(0);
        double avgP99   = latest.values().stream().mapToDouble(MetricsHistoryStore.MetricsSnapshot::p99Ms).average().orElse(0);
        return Map.of(
            "totalRps",        r2(totalRps),
            "avgCpuPct",       r1(avgCpu),
            "avgErrorRatePct", r1(avgErr),
            "avgP99Ms",        (long) avgP99,
            "trackedServices", latest.size()
        );
    }

    @GetMapping("/realtime")
    public Map<String, Object> realtime() {
        Map<String, Object> out = new LinkedHashMap<>();
        store.getLatestAll().forEach((svc, s) -> {
            long heapPct = s.heapMaxMb() > 0 ? Math.round(s.heapUsedMb() / s.heapMaxMb() * 100) : 0;
            out.put(svc, Map.of(
                "cpu",       r1(s.cpuPct()),
                "heapUsed",  (long) s.heapUsedMb(),
                "heapMax",   (long) s.heapMaxMb(),
                "heapPct",   heapPct,
                "threads",   s.threads(),
                "rps",       r2(s.rps()),
                "errorRate", r1(s.errorRatePct()),
                "p99Ms",     (long) s.p99Ms(),
                "ts",        FMT.format(s.timestamp())
            ));
        });
        return out;
    }

    @GetMapping("/history/{service}")
    public Map<String, Object> history(@PathVariable String service) {
        List<MetricsHistoryStore.MetricsSnapshot> snaps = store.getHistory(service);
        List<String> labels = new ArrayList<>();
        List<Double> cpu    = new ArrayList<>();
        List<Double> heap   = new ArrayList<>();
        List<Double> rps    = new ArrayList<>();
        List<Double> errors = new ArrayList<>();
        List<Double> p99    = new ArrayList<>();
        for (MetricsHistoryStore.MetricsSnapshot s : snaps) {
            labels.add(FMT.format(s.timestamp()));
            cpu.add(r1(s.cpuPct()));
            heap.add(s.heapMaxMb() > 0 ? r1(s.heapUsedMb() / s.heapMaxMb() * 100.0) : 0.0);
            rps.add(r2(s.rps()));
            errors.add(r1(s.errorRatePct()));
            p99.add(r1(s.p99Ms()));
        }
        return Map.of("service", service, "labels", labels,
                "cpu", cpu, "heapPct", heap, "rps", rps, "errorRate", errors, "p99Ms", p99);
    }

    @GetMapping("/trends")
    public Map<String, Object> trends() {
        Map<String, List<MetricsHistoryStore.MetricsSnapshot>> all = new LinkedHashMap<>();
        for (String svc : store.getTrackedServices()) all.put(svc, store.getHistory(svc));
        if (all.isEmpty()) return Map.of("labels", List.of(), "datasets", List.of());
        String ref = all.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey).orElse("");
        List<String> labels = all.getOrDefault(ref, List.of()).stream()
                .map(s -> FMT.format(s.timestamp())).toList();
        List<Map<String, Object>> datasets = new ArrayList<>();
        all.forEach((svc, snaps) -> datasets.add(Map.of(
                "service", svc,
                "rps",     snaps.stream().map(s -> r2(s.rps())).toList(),
                "cpu",     snaps.stream().map(s -> r1(s.cpuPct())).toList()
        )));
        return Map.of("labels", labels, "datasets", datasets);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter e = new SseEmitter(300_000L);
        emitters.add(e);
        e.onCompletion(() -> emitters.remove(e));
        e.onTimeout(()    -> emitters.remove(e));
        return e;
    }

    private void pushLive() {
        if (emitters.isEmpty()) return;
        Object data = realtime();
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter e : emitters) {
            try { e.send(SseEmitter.event().name("metrics").data(data)); }
            catch (Exception ex) { dead.add(e); }
        }
        emitters.removeAll(dead);
    }

    private static double r1(double v) { return Math.round(v * 10.0)   / 10.0; }
    private static double r2(double v) { return Math.round(v * 100.0)  / 100.0; }
}
