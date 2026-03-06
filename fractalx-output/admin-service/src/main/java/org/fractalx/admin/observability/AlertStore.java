package org.fractalx.admin.observability;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory alert store with a rolling 500-event buffer.
 */
@Component
public class AlertStore {

    static final int MAX_SIZE = 500;

    private final CopyOnWriteArrayList<AlertEvent> events = new CopyOnWriteArrayList<>();

    public synchronized void save(AlertEvent event) {
        if (events.size() >= MAX_SIZE) events.remove(0);
        events.add(event);
    }

    public List<AlertEvent> findAll(int page, int size) {
        List<AlertEvent> all = new ArrayList<>(events);
        int from = Math.min(page * size, all.size());
        int to   = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    public List<AlertEvent> findUnresolved() {
        return events.stream().filter(e -> !e.isResolved()).collect(Collectors.toList());
    }

    public List<AlertEvent> findBySeverity(String severity) {
        return events.stream()
                .filter(e -> severity.equalsIgnoreCase(e.getSeverity().name()))
                .collect(Collectors.toList());
    }

    public boolean resolve(String id) {
        for (AlertEvent e : events) {
            if (e.getId().equals(id) && !e.isResolved()) {
                e.setResolved(true);
                e.setResolvedAt(Instant.now());
                return true;
            }
        }
        return false;
    }

    public long countUnresolved() {
        return events.stream().filter(e -> !e.isResolved()).count();
    }
}
