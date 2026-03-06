package org.fractalx.admin.model;

import java.util.List;

/** Describes a saga orchestration definition baked in at generation time. */
public class SagaInfo {
    private String       sagaId;
    private String       orchestratedBy;
    private List<String> steps;
    private List<String> compensationSteps;
    private boolean      enabled;

    public String       getSagaId()           { return sagaId; }
    public void         setSagaId(String s)   { this.sagaId = s; }
    public String       getOrchestratedBy()   { return orchestratedBy; }
    public void         setOrchestratedBy(String o) { this.orchestratedBy = o; }
    public List<String> getSteps()            { return steps; }
    public void         setSteps(List<String> s) { this.steps = s; }
    public List<String> getCompensationSteps(){ return compensationSteps; }
    public void         setCompensationSteps(List<String> c) { this.compensationSteps = c; }
    public boolean      isEnabled()           { return enabled; }
    public void         setEnabled(boolean e) { this.enabled = e; }
}
