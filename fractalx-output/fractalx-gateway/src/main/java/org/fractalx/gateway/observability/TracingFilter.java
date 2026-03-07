package org.fractalx.gateway.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propagates correlation and W3C trace-context headers through the gateway,
 * and tags the active OTel span with the correlation ID for Jaeger search.
 * <ul>
 *   <li>X-Request-Id     — fresh UUID per request</li>
 *   <li>X-Correlation-Id — propagated from upstream if present, else equals X-Request-Id</li>
 *   <li>correlation.id   — added as OTel span attribute for Jaeger tag search</li>
 * </ul>
 * With micrometer-tracing-bridge-otel on the classpath, Spring Boot auto-configures
 * traceparent propagation to downstream services via Reactor Netty HTTP client.
 */
@Component
public class TracingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() { return -99; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId     = UUID.randomUUID().toString();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = requestId;
        }
        final String finalCorrelationId = correlationId;

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.set("X-Request-Id",     requestId);
                    h.set("X-Correlation-Id", finalCorrelationId);
                }))
                .build();

        // Register response headers to be set before the response is committed
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set("X-Request-Id",     requestId);
            mutated.getResponse().getHeaders().set("X-Correlation-Id", finalCorrelationId);
            return Mono.empty();
        });

        // Tag the active OTel span with the correlation ID synchronously.
        // ServerHttpObservationFilter (WebFilter, ordered at MIN_VALUE+1) runs before
        // all GlobalFilters, so the span is already started when we reach this point.
        // Span.current() is reliable here on the calling thread.
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAttribute(AttributeKey.stringKey("correlation.id"), finalCorrelationId);
        }

        return chain.filter(mutated);
    }
}
