package org.fractalx.logger;

import java.time.Instant;

/** Structured log entry shipped from a microservice via FractalLogAppender. */
public class LogEntry {

    private String  service;
    private String  correlationId;
    private String  spanId;
    private String  parentSpanId;
    private String  level;
    private String  message;
    private Instant timestamp;
    private String  receivedAt;

    public String  getService()       { return service; }
    public void    setService(String s) { this.service = s; }

    public String  getCorrelationId()           { return correlationId; }
    public void    setCorrelationId(String c)   { this.correlationId = c; }

    public String  getSpanId()                  { return spanId; }
    public void    setSpanId(String s)          { this.spanId = s; }

    public String  getParentSpanId()            { return parentSpanId; }
    public void    setParentSpanId(String p)    { this.parentSpanId = p; }

    public String  getLevel()                   { return level; }
    public void    setLevel(String l)           { this.level = l; }

    public String  getMessage()                 { return message; }
    public void    setMessage(String m)         { this.message = m; }

    public Instant getTimestamp()               { return timestamp; }
    public void    setTimestamp(Instant t)      { this.timestamp = t; }

    public String  getReceivedAt()              { return receivedAt; }
    public void    setReceivedAt(String t)      { this.receivedAt = t; }
}
