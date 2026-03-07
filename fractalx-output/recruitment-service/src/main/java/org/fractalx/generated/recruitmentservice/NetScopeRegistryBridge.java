package org.fractalx.generated.recruitmentservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "fractalx.registry.enabled", havingValue = "true", matchIfMissing = true)
public class NetScopeRegistryBridge {

    private static final Logger log = LoggerFactory.getLogger(NetScopeRegistryBridge.class);

    private final ConfigurableEnvironment environment;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    public NetScopeRegistryBridge(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void resolvePeers() {
                resolveAndUpdate("department-service");
        resolveAndUpdate("employee-service");

    }

    private void resolveAndUpdate(String serviceName) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> reg = restTemplate.getForObject(
                        registryUrl + "/services/" + serviceName, Map.class);
                if (reg != null && reg.containsKey("host")) {
                    String host = (String) reg.get("host");
                    int grpcPort = ((Number) reg.get("grpcPort")).intValue();
                    Map<String, Object> props = new HashMap<>();
                    props.put("netscope.client.servers." + serviceName + ".host", host);
                    props.put("netscope.client.servers." + serviceName + ".port", grpcPort);
                    environment.getPropertySources().addFirst(
                            new MapPropertySource("fractalx-registry-" + serviceName, props));
                    log.info("Resolved {} -> {}:{} via registry", serviceName, host, grpcPort);
                    return;
                }
            } catch (Exception e) {
                log.warn("Registry lookup for {} failed (attempt {}/5): {}",
                        serviceName, attempt, e.getMessage());
            }
            try { Thread.sleep(2000L * attempt); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
        }
        log.warn("Could not resolve {} from registry — using static YAML fallback", serviceName);
    }
}
