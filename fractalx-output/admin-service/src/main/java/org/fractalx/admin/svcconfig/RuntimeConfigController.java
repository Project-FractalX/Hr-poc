package org.fractalx.admin.svcconfig;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exposes the platform-level configuration baked in at generation time and allows
 * in-memory overrides that are visible to the admin dashboard.
 *
 * <pre>
 * GET    /api/config/runtime          — generation defaults + current overrides + effective values
 * PUT    /api/config/runtime/{key}    — set an in-memory override (body: {"value":"..."})
 * DELETE /api/config/runtime/{key}    — remove override (revert to generation default)
 * GET    /api/config/runtime/diff     — list keys that differ from generation defaults
 * </pre>
 *
 * <p>Overrides are stored in memory only and reset on service restart.
 * To make changes permanent, update the source application.yml / fractalx-config.yml
 * and re-run {@code mvn fractalx:decompose}.
 */
@RestController
@RequestMapping("/api/config/runtime")
@CrossOrigin(origins = "*")
public class RuntimeConfigController {

    /** Values captured from the pre-decomposed application config at generation time. */
    private static final Map<String, String> GENERATION_DEFAULTS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("registryUrl",        "http://localhost:8761");
        m.put("loggerUrl",          "http://localhost:9099");
        m.put("otelEndpoint",       "http://localhost:4317");
        m.put("gatewayPort",        "9999");
        m.put("adminPort",          "9090");
        m.put("corsAllowedOrigins", "http://localhost:3000,http://localhost:4200");
        m.put("oauth2JwksUri",      "http://localhost:8080/realms/fractalx/protocol/openid-connect/certs");
        GENERATION_DEFAULTS = Collections.unmodifiableMap(m);
    }

    /** In-memory overrides — reset on service restart. */
    private final Map<String, String> overrides = new ConcurrentHashMap<>();

    /** Returns defaults, current overrides, and the effective merged view. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRuntimeConfig() {
        Map<String, String> effective = new LinkedHashMap<>(GENERATION_DEFAULTS);
        effective.putAll(overrides);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generationDefaults", GENERATION_DEFAULTS);
        result.put("overrides", overrides);
        result.put("effective", effective);
        return ResponseEntity.ok(result);
    }

    /** Stores an in-memory override for a config key. */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, String>> setOverride(
            @PathVariable("key") String key,
            @RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        overrides.put(key, value);
        return ResponseEntity.ok(Map.of("key", key, "value", value, "status", "overridden"));
    }

    /** Removes an in-memory override, reverting to the generation-time default. */
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, String>> removeOverride(
            @PathVariable("key") String key) {
        overrides.remove(key);
        return ResponseEntity.ok(Map.of("key", key, "status", "reset-to-default"));
    }

    /** Returns keys whose current effective value differs from the generation default. */
    @GetMapping("/diff")
    public ResponseEntity<List<Map<String, String>>> getDiff() {
        List<Map<String, String>> diffs = new ArrayList<>();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String def = GENERATION_DEFAULTS.getOrDefault(entry.getKey(), "(not set)");
            if (!def.equals(entry.getValue())) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("key",      entry.getKey());
                row.put("default",  def);
                row.put("override", entry.getValue());
                diffs.add(row);
            }
        }
        return ResponseEntity.ok(diffs);
    }
}
