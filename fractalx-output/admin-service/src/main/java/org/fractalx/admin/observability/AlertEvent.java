package org.fractalx.admin.observability;

import java.time.Instant;
import java.util.UUID;

/** An alert that was fired by the {@link AlertEvaluator}. */
public class AlertEvent {

    private final String        id        = UUID.randomUUID().toString();
    private final Instant       timestamp = Instant.now();
    private String              service;
    private AlertRule           rule;
    private AlertSeverity       severity;
    private String              message;
    private boolean             resolved  = false;
    private Instant             resolvedAt;

    public String        getId()                      { return id; }
    public Instant       getTimestamp()               { return timestamp; }
    public String        getService()                 { return service; }
    public void          setService(String s)         { this.service = s; }
    public AlertRule     getRule()                    { return rule; }
    public void          setRule(AlertRule r)         { this.rule = r; }
    public AlertSeverity getSeverity()                { return severity; }
    public void          setSeverity(AlertSeverity s) { this.severity = s; }
    public String        getMessage()                 { return message; }
    public void          setMessage(String m)         { this.message = m; }
    public boolean       isResolved()                 { return resolved; }
    public void          setResolved(boolean r)       { this.resolved = r; }
    public Instant       getResolvedAt()              { return resolvedAt; }
    public void          setResolvedAt(Instant t)     { this.resolvedAt = t; }
}
