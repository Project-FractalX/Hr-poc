package org.fractalx.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate limit config. Override per-service via:
 * <pre>
 * fractalx:
 *   gateway:
 *     rate-limit:
 *       default-rps: 100
 *       per-service:
 *         order-service: 200
 *         payment-service: 50
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "fractalx.gateway.rate-limit")
public class RateLimitConfig {

    private int defaultRps = 100;
    private Map<String, Integer> perService = new HashMap<>();

    public int getDefaultRps() { return defaultRps; }
    public void setDefaultRps(int rps) { this.defaultRps = rps; }
    public Map<String, Integer> getPerService() { return perService; }
    public void setPerService(Map<String, Integer> m) { this.perService = m; }

    public int getRpsForService(String serviceName) {
        return perService.getOrDefault(serviceName, defaultRps);
    }
}
