package org.fractalx.admin.model;

/** Represents a NetScope/gRPC communication link between two generated services. */
public class NetScopeLink {
    private String sourceService;
    private String targetService;
    private int    grpcPort;
    private String protocol;

    public NetScopeLink(String sourceService, String targetService, int grpcPort) {
        this.sourceService = sourceService;
        this.targetService = targetService;
        this.grpcPort      = grpcPort;
        this.protocol      = "NetScope/gRPC";
    }

    public String getSourceService() { return sourceService; }
    public String getTargetService() { return targetService; }
    public int    getGrpcPort()      { return grpcPort; }
    public String getProtocol()      { return protocol; }
}
