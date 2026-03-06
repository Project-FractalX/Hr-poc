package org.fractalx.admin.svcconfig;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for the Configuration section of the admin dashboard.
 *
 * <pre>
 * GET /api/config/services         — all service configurations
 * GET /api/config/services/{name}  — single service configuration
 * GET /api/config/environment      — all env vars grouped by service
 * GET /api/config/ports            — port mapping summary (HTTP + gRPC)
 * GET /api/config/commands/{name}  — docker-compose lifecycle commands
 * </pre>
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ServiceConfigStore configStore;

    public ConfigController(ServiceConfigStore configStore) {
        this.configStore = configStore;
    }

    /** Returns the full configuration list for all services. */
    @GetMapping("/services")
    public ResponseEntity<List<ServiceConfigStore.ServiceConfig>> getAllConfigs() {
        return ResponseEntity.ok(configStore.getAll());
    }

    /** Returns the configuration for a single service by name. */
    @GetMapping("/services/{name}")
    public ResponseEntity<ServiceConfigStore.ServiceConfig> getServiceConfig(
            @PathVariable("name") String name) {
        return configStore.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Returns all environment variables grouped by service name. */
    @GetMapping("/environment")
    public ResponseEntity<Map<String, Map<String, String>>> getEnvironment() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (ServiceConfigStore.ServiceConfig cfg : configStore.getAll()) {
            result.put(cfg.name(), cfg.envVars());
        }
        return ResponseEntity.ok(result);
    }

    /** Returns HTTP and gRPC port mapping for all services. */
    @GetMapping("/ports")
    public ResponseEntity<List<Map<String, Object>>> getPortMapping() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceConfigStore.ServiceConfig cfg : configStore.getAll()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",     cfg.name());
            entry.put("httpPort", cfg.httpPort());
            entry.put("grpcPort", cfg.grpcPort());
            entry.put("hasOutbox",cfg.hasOutbox());
            entry.put("hasSaga",  cfg.hasSaga());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /** Returns docker-compose lifecycle commands for a service. */
    @GetMapping("/commands/{name}")
    public ResponseEntity<Map<String, String>> getLifecycleCommands(
            @PathVariable("name") String name) {
        Map<String, String> cmds = new LinkedHashMap<>();
        cmds.put("start",   "docker compose up -d " + name);
        cmds.put("stop",    "docker compose stop " + name);
        cmds.put("restart", "docker compose restart " + name);
        cmds.put("logs",    "docker compose logs -f " + name);
        cmds.put("build",   "docker compose build " + name);
        cmds.put("pull",    "docker compose pull " + name);
        return ResponseEntity.ok(cmds);
    }
}
