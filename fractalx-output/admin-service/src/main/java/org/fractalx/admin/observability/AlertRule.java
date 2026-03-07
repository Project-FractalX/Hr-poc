package org.fractalx.admin.observability;

/**
 * Defines a single alerting rule.
 *
 * <p>Supported conditions: {@code health}, {@code response-time}, {@code error-rate}.
 */
public class AlertRule {

    private String        name;
    private String        condition;
    private double        threshold;
    private AlertSeverity severity          = AlertSeverity.WARNING;
    private boolean       enabled           = true;
    private int           consecutiveFailures = 1;

    public String        getName()                       { return name; }
    public void          setName(String n)               { this.name = n; }
    public String        getCondition()                  { return condition; }
    public void          setCondition(String c)          { this.condition = c; }
    public double        getThreshold()                  { return threshold; }
    public void          setThreshold(double t)          { this.threshold = t; }
    public AlertSeverity getSeverity()                   { return severity; }
    public void          setSeverity(AlertSeverity s)    { this.severity = s; }
    public boolean       isEnabled()                     { return enabled; }
    public void          setEnabled(boolean e)           { this.enabled = e; }
    public int           getConsecutiveFailures()        { return consecutiveFailures; }
    public void          setConsecutiveFailures(int c)   { this.consecutiveFailures = c; }
}
