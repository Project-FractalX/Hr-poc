package org.fractalx.registry.controller;

import org.fractalx.registry.model.ServiceRegistration;
import org.fractalx.registry.service.RegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/services")
public class RegistryController {

    private final RegistryService registryService;

    public RegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping
    public ResponseEntity<ServiceRegistration> register(@RequestBody ServiceRegistration reg) {
        return ResponseEntity.ok(registryService.register(reg));
    }

    @GetMapping
    public ResponseEntity<Collection<ServiceRegistration>> getAll() {
        return ResponseEntity.ok(registryService.findAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<ServiceRegistration> getByName(@PathVariable String name) {
        return registryService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{name}/deregister")
    public ResponseEntity<Void> deregister(@PathVariable String name) {
        registryService.deregister(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{name}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable String name) {
        registryService.heartbeat(name);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        long upCount = registryService.findAll().stream()
                .filter(r -> "UP".equals(r.getStatus())).count();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "registeredServices", registryService.findAll().size(),
                "upServices", upCount
        ));
    }
}
