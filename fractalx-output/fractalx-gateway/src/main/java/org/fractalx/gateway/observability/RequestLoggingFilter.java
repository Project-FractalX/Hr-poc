package org.fractalx.gateway.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Structured request/response logging at the gateway edge.
 * Logs: method, path, correlationId, response status, and duration in ms.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public int getOrder() { return -100; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long   startTime     = System.currentTimeMillis();
        String method        = exchange.getRequest().getMethod().name();
        String path          = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        log.info("[GATEWAY] --> {} {} correlationId={}", method, path,
                correlationId != null ? correlationId : "-");

        return chain.filter(exchange).doFinally(signal -> {
            long duration = System.currentTimeMillis() - startTime;
            int  status   = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            log.info("[GATEWAY] <-- {} {} status={} duration={}ms correlationId={}",
                    method, path, status, duration,
                    correlationId != null ? correlationId : "-");
        });
    }
}
