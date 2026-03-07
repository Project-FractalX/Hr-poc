package org.fractalx.admin.incidents;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Thread-safe in-memory incident store. Pre-seeded with one example incident. */
@Component
public class IncidentStore {

    private final ConcurrentHashMap<String, Incident> incidents = new ConcurrentHashMap<>();

    public IncidentStore() {
        // Seed one example resolved incident
        String id = UUID.randomUUID().toString();
        incidents.put(id, new Incident(
                id, "High latency on payment-service",
                "P99 response time exceeded 2s threshold during peak load.",
                "P2", "RESOLVED", "payment-service",
                "Resolved by increasing connection pool size.", "admin",
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800),
                Instant.now().minusSeconds(1800)));
    }

    public List<Incident> getAll() {
        return incidents.values().stream()
                .sorted(Comparator.comparing(Incident::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Incident> getOpen() {
        return incidents.values().stream()
                .filter(i -> "OPEN".equals(i.status()) || "INVESTIGATING".equals(i.status()))
                .sorted(Comparator.comparing(Incident::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Incident> findById(String id) {
        return Optional.ofNullable(incidents.get(id));
    }

    public Incident save(Incident incident) {
        incidents.put(incident.id(), incident);
        return incident;
    }

    public boolean delete(String id) {
        return incidents.remove(id) != null;
    }

    public Map<String, Object> stats() {
        Collection<Incident> all = incidents.values();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Incident::status, Collectors.counting()));
        Map<String, Long> bySev = all.stream()
                .collect(Collectors.groupingBy(Incident::severity, Collectors.counting()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total",         all.size());
        out.put("open",          byStatus.getOrDefault("OPEN", 0L));
        out.put("investigating", byStatus.getOrDefault("INVESTIGATING", 0L));
        out.put("resolved",      byStatus.getOrDefault("RESOLVED", 0L));
        out.put("bySeverity",    bySev);
        return out;
    }
}
