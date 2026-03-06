package org.fractalx.admin.incidents;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

/**
 * Incident management REST API.
 * All state is in-memory; restarts clear incidents except the seeded example.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentStore store;

    public IncidentController(IncidentStore store) { this.store = store; }

    @GetMapping
    public List<Incident> getAll() { return store.getAll(); }

    @GetMapping("/open")
    public List<Incident> getOpen() { return store.getOpen(); }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return store.stats(); }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getOne(@PathVariable String id) {
        return store.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Incident create(@RequestBody Map<String, Object> body) {
        String id  = UUID.randomUUID().toString();
        String sev = str(body, "severity", "P3");
        Incident inc = new Incident(
                id,
                str(body, "title", "Untitled Incident"),
                str(body, "description", ""),
                sev, "OPEN",
                str(body, "affectedService", ""),
                str(body, "notes", ""),
                str(body, "assignee", ""),
                Instant.now(), Instant.now(), null);
        return store.save(inc);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Incident> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return store.findById(id)
                .map(inc -> {
                    Incident updated = inc.withStatus(
                            str(body, "status", inc.status()),
                            str(body, "notes", null));
                    return ResponseEntity.ok(store.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Incident> update(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        return store.findById(id)
                .map(inc -> {
                    Incident updated = inc.withUpdate(
                            str(body, "title", null),
                            str(body, "description", null),
                            str(body, "severity", null),
                            str(body, "affectedService", null),
                            str(body, "assignee", null));
                    return ResponseEntity.ok(store.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return store.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return (v != null && !String.valueOf(v).isBlank()) ? String.valueOf(v) : def;
    }
}
