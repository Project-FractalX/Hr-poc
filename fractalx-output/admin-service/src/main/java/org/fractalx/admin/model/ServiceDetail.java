package org.fractalx.admin.model;

import java.util.List;
import java.util.Map;

/** Extended service metadata combining static config with live health status. */
public class ServiceDetail {
    private String              name;
    private int                 port;
    private int                 grpcPort;
    private String              type;
    private List<String>        dependencies;
    private String              healthStatus;    // UP | DOWN | UNKNOWN
    private Map<String, String> lifecycleCommands;

    public String              getName()               { return name; }
    public void                setName(String n)       { this.name = n; }
    public int                 getPort()               { return port; }
    public void                setPort(int p)          { this.port = p; }
    public int                 getGrpcPort()           { return grpcPort; }
    public void                setGrpcPort(int g)      { this.grpcPort = g; }
    public String              getType()               { return type; }
    public void                setType(String t)       { this.type = t; }
    public List<String>        getDependencies()       { return dependencies; }
    public void                setDependencies(List<String> d) { this.dependencies = d; }
    public String              getHealthStatus()       { return healthStatus; }
    public void                setHealthStatus(String h){ this.healthStatus = h; }
    public Map<String, String> getLifecycleCommands()  { return lifecycleCommands; }
    public void                setLifecycleCommands(Map<String, String> c) { this.lifecycleCommands = c; }
}
