package org.fractalx.logger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API for the centralized log aggregation service.
 *
 * <pre>
 * POST /api/logs                          — ingest a log entry
 * GET  /api/logs                          — query logs (paged, filterable)
 * GET  /api/logs/services                 — list known service names
 * GET  /api/logs/stats                    — per-service totals and error counts
 * </pre>
 */
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    private final LogRepository repository;

    public LogController(LogRepository repository) {
        this.repository = repository;
    }

    /** Ingest a structured log entry from a microservice. */
    @PostMapping
    public ResponseEntity<Void> ingestLog(@RequestBody LogEntry entry) {
        if (entry.getReceivedAt() == null) {
            entry.setReceivedAt(Instant.now().toString());
        }
        repository.save(entry);
        return ResponseEntity.accepted().build();
    }

    /**
     * Query logs with optional filters.
     *
     * @param correlationId filter by correlation ID
     * @param service       filter by service name
     * @param level         filter by log level (INFO, WARN, ERROR, …)
     * @param page          zero-based page number (default 0)
     * @param size          page size (default 50, max 200)
     */
    @GetMapping
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        return ResponseEntity.ok(repository.query(correlationId, service, level, page, safeSize));
    }

    /** List all service names that have shipped at least one log entry. */
    @GetMapping("/services")
    public ResponseEntity<List<String>> getServices() {
        return ResponseEntity.ok(repository.findDistinctServices());
    }

    /** Per-service log totals and error counts. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Map<String, Long>>> getStats() {
        return ResponseEntity.ok(repository.stats());
    }
}
