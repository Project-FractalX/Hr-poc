package org.fractalx.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates Bearer JWT tokens signed with HMAC-SHA256.
 * Active when {@code fractalx.gateway.security.bearer.enabled=true}.
 * Injects X-User-Id and X-User-Roles headers downstream on success.
 * Registered automatically by Spring Cloud Gateway as a GlobalFilter bean.
 */
@Component
public class JwtBearerFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtBearerFilter.class);

    private final GatewayAuthProperties props;

    public JwtBearerFilter(GatewayAuthProperties props) {
        this.props = props;
    }

    @Override
    public int getOrder() { return -90; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isEnabled() || !props.getBearer().isEnabled()) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange); // let other filters decide
        }
        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    props.getBearer().getJwtSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            String roles = claims.get("roles", String.class);
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.set("X-User-Id",    claims.getSubject());
                        h.set("X-User-Roles", roles != null ? roles : "");
                        h.set("X-Auth-Method", "bearer-jwt");
                    }))
                    .build();
            return chain.filter(mutated);
        } catch (JwtException e) {
            log.warn("Invalid Bearer JWT: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
