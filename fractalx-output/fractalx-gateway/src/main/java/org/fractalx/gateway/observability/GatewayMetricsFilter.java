package org.fractalx.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Records per-service request counts and latency histograms via Micrometer.
 * Metrics:
 * <ul>
 *   <li>{@code gateway.requests.total{service, method, status}} — counter</li>
 *   <li>{@code gateway.requests.duration{service}}               — timer (ms)</li>
 * </ul>
 * These metrics are scraped from {@code /actuator/metrics} and exposed via
 * {@code GET /api/observability/metrics} in the admin service.
 */
@Component
public class GatewayMetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() { return -98; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long   start   = System.currentTimeMillis();
        String path    = exchange.getRequest().getPath().value();
        String service = extractServiceName(path);
        String method  = exchange.getRequest().getMethod().name();

        return chain.filter(exchange).doFinally(signal -> {
            long   duration = System.currentTimeMillis() - start;
            String status   = exchange.getResponse().getStatusCode() != null
                    ? String.valueOf(exchange.getResponse().getStatusCode().value()) : "0";

            Counter.builder("gateway.requests.total")
                    .tag("service", service)
                    .tag("method",  method)
                    .tag("status",  status)
                    .register(meterRegistry)
                    .increment();

            Timer.builder("gateway.requests.duration")
                    .tag("service", service)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
        });
    }

    private String extractServiceName(String path) {
        // e.g. /api/orders/123 -> "orders"
        String[] parts = path.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }
}
