package org.fractalx.admin.controller;

import org.fractalx.admin.model.ServiceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
public class DashboardController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<ServiceInfo> services = getServiceStatuses();
        model.addAttribute("services", services);
        model.addAttribute("totalServices", services.size());
        model.addAttribute("runningServices",
                services.stream().filter(ServiceInfo::isRunning).count());
        return "dashboard";
    }

    private List<ServiceInfo> getServiceStatuses() {
        List<ServiceInfo> services = new ArrayList<>();
        services.add(new ServiceInfo("department-service", "http://localhost:8085", checkServiceHealth("http://localhost:8085/actuator/health")));
services.add(new ServiceInfo("employee-service", "http://localhost:8081", checkServiceHealth("http://localhost:8081/actuator/health")));
services.add(new ServiceInfo("leave-service", "http://localhost:8083", checkServiceHealth("http://localhost:8083/actuator/health")));
services.add(new ServiceInfo("payroll-service", "http://localhost:8082", checkServiceHealth("http://localhost:8082/actuator/health")));
services.add(new ServiceInfo("recruitment-service", "http://localhost:8084", checkServiceHealth("http://localhost:8084/actuator/health")));

        return services;
    }

    private boolean checkServiceHealth(String url) {
        try {
            String resp = restTemplate.getForObject(url, String.class);
            return resp != null && resp.contains("UP");
        } catch (Exception e) {
            return false;
        }
    }
}
