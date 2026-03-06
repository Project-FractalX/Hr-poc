package org.fractalx.admin.analytics;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe circular buffer: up to 60 metric snapshots per service.
 * At 15-second collection intervals this covers ~15 minutes of history.
 */
@Component
public class MetricsHistoryStore {

    public record MetricsSnapshot(
            Instant timestamp,
            double  cpuPct,
            double  heapUsedMb,
            double  heapMaxMb,
            long    threads,
            double  rps,
            double  errorRatePct,
            double  p99Ms
    ) {}

    private static final int MAX_POINTS = 60;
    private final ConcurrentHashMap<String, ArrayDeque<MetricsSnapshot>> history =
            new ConcurrentHashMap<>();

    public void record(String service, MetricsSnapshot snapshot) {
        history.compute(service, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>(MAX_POINTS + 1);
            deque.addLast(snapshot);
            if (deque.size() > MAX_POINTS) deque.pollFirst();
            return deque;
        });
    }

    public List<MetricsSnapshot> getHistory(String service) {
        ArrayDeque<MetricsSnapshot> deque = history.get(service);
        return deque == null ? List.of() : new ArrayList<>(deque);
    }

    public MetricsSnapshot getLatest(String service) {
        ArrayDeque<MetricsSnapshot> deque = history.get(service);
        return (deque == null || deque.isEmpty()) ? null : deque.peekLast();
    }

    public Map<String, MetricsSnapshot> getLatestAll() {
        Map<String, MetricsSnapshot> out = new LinkedHashMap<>();
        history.forEach((svc, deque) -> {
            if (!deque.isEmpty()) out.put(svc, deque.peekLast());
        });
        return out;
    }

    public Set<String> getTrackedServices() { return history.keySet(); }
}
