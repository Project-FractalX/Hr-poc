package org.fractalx.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Validates HTTP Basic / Simple Auth credentials.
 * Active when {@code fractalx.gateway.security.basic.enabled=true}.
 * Registered automatically by Spring Cloud Gateway as a GlobalFilter bean.
 */
@Component
public class BasicAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthGatewayFilter.class);

    private final GatewayAuthProperties props;

    public BasicAuthGatewayFilter(GatewayAuthProperties props) {
        this.props = props;
    }

    @Override
    public int getOrder() { return -85; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isEnabled() || !props.getBasic().isEnabled()) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return chain.filter(exchange);
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(authHeader.substring(6)),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length == 2
                    && props.getBasic().getUsername().equals(parts[0])
                    && props.getBasic().getPassword().equals(parts[1])) {
                ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r.headers(h -> {
                            h.set("X-User-Id",    parts[0]);
                            h.set("X-Auth-Method", "basic");
                        }))
                        .build();
                return chain.filter(mutated);
            }
        } catch (Exception e) {
            log.warn("Malformed Basic Auth header: {}", e.getMessage());
        }
        log.warn("Invalid Basic Auth credentials from {}", exchange.getRequest().getRemoteAddress());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
