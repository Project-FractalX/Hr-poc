package org.fractalx.logger;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory log store with a rolling 5000-entry buffer.
 * Indexed by correlationId and service name for efficient querying.
 */
@Component
public class LogRepository {

    static final int MAX_SIZE = 5_000;

    private final List<LogEntry> storage = new CopyOnWriteArrayList<>();

    public synchronized void save(LogEntry entry) {
        if (storage.size() >= MAX_SIZE) {
            storage.remove(0);
        }
        storage.add(entry);
    }

    public List<LogEntry> findAll(int page, int size) {
        List<LogEntry> all = new ArrayList<>(storage);
        int from = Math.min(page * size, all.size());
        int to   = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    public List<LogEntry> findByCorrelationId(String correlationId) {
        return storage.stream()
                .filter(e -> correlationId.equals(e.getCorrelationId()))
                .collect(Collectors.toList());
    }

    public List<LogEntry> findByService(String service) {
        return storage.stream()
                .filter(e -> service.equals(e.getService()))
                .collect(Collectors.toList());
    }

    public List<LogEntry> findByLevel(String level) {
        return storage.stream()
                .filter(e -> level.equalsIgnoreCase(e.getLevel()))
                .collect(Collectors.toList());
    }

    public List<LogEntry> query(String correlationId, String service,
                                String level, int page, int size) {
        return storage.stream()
                .filter(e -> correlationId == null || correlationId.equals(e.getCorrelationId()))
                .filter(e -> service       == null || service.equals(e.getService()))
                .filter(e -> level         == null || level.equalsIgnoreCase(e.getLevel()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    public List<String> findDistinctServices() {
        return storage.stream()
                .map(LogEntry::getService)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Per-service log counts and ERROR counts for the stats endpoint. */
    public Map<String, Map<String, Long>> stats() {
        return storage.stream()
                .filter(e -> e.getService() != null)
                .collect(Collectors.groupingBy(
                        LogEntry::getService,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> Map.of(
                                        "total",  (long) list.size(),
                                        "errors", list.stream()
                                                .filter(e -> "ERROR".equalsIgnoreCase(e.getLevel()))
                                                .count()
                                )
                        )
                ));
    }
}
