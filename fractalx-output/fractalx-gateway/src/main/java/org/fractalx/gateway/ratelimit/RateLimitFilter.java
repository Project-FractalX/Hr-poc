package org.fractalx.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window in-memory rate limiter per remote IP + service path.
 * No Redis required — suitable for single-instance gateway.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitConfig config;
    // key: "ip:service", value: [windowStartMs, count]
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public int getOrder() { return -80; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path    = exchange.getRequest().getPath().value();
        String svcName = extractServiceName(path);
        String ip      = extractIp(exchange);
        String key     = ip + ":" + svcName;
        int    limit   = config.getRpsForService(svcName);

        long now    = System.currentTimeMillis();
        long[] slot = windows.computeIfAbsent(key, k -> new long[]{now, 0});

        synchronized (slot) {
            if (now - slot[0] > 1000L) {
                slot[0] = now;
                slot[1] = 0;
            }
            slot[1]++;
            if (slot[1] > limit) {
                log.warn("Rate limit exceeded: ip={} service={} count={}", ip, svcName, slot[1]);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().set("Retry-After", "1");
                exchange.getResponse().getHeaders().set("X-RateLimit-Limit",     String.valueOf(limit));
                exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", "0");
                return exchange.getResponse().setComplete();
            }
            exchange.getResponse().getHeaders()
                    .set("X-RateLimit-Remaining", String.valueOf(limit - slot[1]));
        }
        return chain.filter(exchange);
    }

    private String extractServiceName(String path) {
        String[] parts = path.split("/");
        return parts.length > 2 ? parts[2] : "default";
    }

    private String extractIp(ServerWebExchange exchange) {
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }
}
