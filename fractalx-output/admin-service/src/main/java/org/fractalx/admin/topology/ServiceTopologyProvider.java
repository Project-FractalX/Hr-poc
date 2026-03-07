package org.fractalx.admin.topology;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Baked-in service topology graph — nodes and edges derived at generation time. */
@Component
public class ServiceTopologyProvider {

    public TopologyGraph getTopology() {
        List<TopologyGraph.ServiceNode> nodes = new ArrayList<>();
        List<TopologyGraph.ServiceEdge> edges = new ArrayList<>();

        nodes.add(new TopologyGraph.ServiceNode("department-service", "department-service", 8085, "microservice"));
        nodes.add(new TopologyGraph.ServiceNode("employee-service", "employee-service", 8081, "microservice"));
        nodes.add(new TopologyGraph.ServiceNode("leave-service", "leave-service", 8083, "microservice"));
        nodes.add(new TopologyGraph.ServiceNode("payroll-service", "payroll-service", 8082, "microservice"));
        nodes.add(new TopologyGraph.ServiceNode("recruitment-service", "recruitment-service", 8084, "microservice"));
        nodes.add(new TopologyGraph.ServiceNode("fractalx-gateway",   "API Gateway",     9999, "gateway"));
        nodes.add(new TopologyGraph.ServiceNode("fractalx-registry",  "Service Registry", 8761, "registry"));
        nodes.add(new TopologyGraph.ServiceNode("admin-service",       "Admin Dashboard",  9090, "admin"));

        edges.add(new TopologyGraph.ServiceEdge("employee-service", "department-service", "grpc"));
        edges.add(new TopologyGraph.ServiceEdge("employee-service", "payroll-service", "grpc"));
        edges.add(new TopologyGraph.ServiceEdge("leave-service", "payroll-service", "grpc"));
        edges.add(new TopologyGraph.ServiceEdge("leave-service", "employee-service", "grpc"));
        edges.add(new TopologyGraph.ServiceEdge("recruitment-service", "department-service", "grpc"));
        edges.add(new TopologyGraph.ServiceEdge("recruitment-service", "employee-service", "grpc"));

        return new TopologyGraph(nodes, edges);
    }
}
