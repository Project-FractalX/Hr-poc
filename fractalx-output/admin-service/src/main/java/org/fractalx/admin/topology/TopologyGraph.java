package org.fractalx.admin.topology;

import java.util.List;

public class TopologyGraph {

    private final List<ServiceNode> nodes;
    private final List<ServiceEdge> edges;

    public TopologyGraph(List<ServiceNode> nodes, List<ServiceEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<ServiceNode> getNodes() { return nodes; }
    public List<ServiceEdge> getEdges() { return edges; }

    public record ServiceNode(String id, String label, int port, String type) {}
    public record ServiceEdge(String source, String target, String protocol) {}
}
