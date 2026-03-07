package org.fractalx.admin.incidents;

import java.time.Instant;

/**
 * Represents a production incident.
 * Immutable — use {@link Incident#withStatus} to transition.
 */
public record Incident(
        String  id,
        String  title,
        String  description,
        String  severity,         // P1 | P2 | P3 | P4
        String  status,           // OPEN | INVESTIGATING | RESOLVED
        String  affectedService,
        String  notes,
        String  assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
    public Incident withStatus(String newStatus, String newNotes) {
        return new Incident(id, title, description, severity, newStatus,
                affectedService, newNotes != null ? newNotes : notes,
                assignee, createdAt, Instant.now(),
                "RESOLVED".equals(newStatus) ? Instant.now() : resolvedAt);
    }
    public Incident withUpdate(String newTitle, String newDesc,
            String newSeverity, String newService, String newAssignee) {
        return new Incident(id,
                newTitle != null ? newTitle : title,
                newDesc  != null ? newDesc  : description,
                newSeverity != null ? newSeverity : severity,
                status,
                newService  != null ? newService  : affectedService,
                notes,
                newAssignee != null ? newAssignee : assignee,
                createdAt, Instant.now(), resolvedAt);
    }
}
