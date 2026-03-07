package org.fractalx.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Validates API Key authentication.
 * Accepts the key via {@code X-Api-Key} header or {@code api_key} query param.
 * Active when {@code fractalx.gateway.security.api-key.enabled=true}.
 * Registered automatically by Spring Cloud Gateway as a GlobalFilter bean.
 */
@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final GatewayAuthProperties props;

    public ApiKeyFilter(GatewayAuthProperties props) {
        this.props = props;
    }

    @Override
    public int getOrder() { return -95; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isEnabled() || !props.getApiKey().isEnabled()) {
            return chain.filter(exchange);
        }
        String key = exchange.getRequest().getHeaders().getFirst("X-Api-Key");
        if (key == null) {
            key = exchange.getRequest().getQueryParams().getFirst("api_key");
        }
        List<String> validKeys = props.getApiKey().getValidKeys();
        if (key != null && validKeys.contains(key)) {
            final String finalKey = key;
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.set("X-Auth-Method", "api-key");
                        h.set("X-Api-Client", finalKey.substring(0,
                                Math.min(6, finalKey.length())) + "***");
                    }))
                    .build();
            return chain.filter(mutated);
        }
        if (key != null) {
            log.warn("Invalid API key from {}", exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange); // no key present — let other filters handle
    }
}
