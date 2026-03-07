package org.fractalx.gateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewaySecurityConfig {

    private final GatewayAuthProperties authProps;

    public GatewaySecurityConfig(GatewayAuthProperties authProps) {
        this.authProps = authProps;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        // Always public — actuator, discovery, docs, fallback, and configured paths
        http.authorizeExchange(ex -> ex
                .pathMatchers("/actuator/health", "/actuator/info",
                        "/services/**", "/api-docs/**", "/swagger-ui/**",
                        "/fallback/**").permitAll()
                .pathMatchers(authProps.getPublicPaths()).permitAll()
                .anyExchange().permitAll()
        );

        // Auth GlobalFilter beans (auto-registered by Spring Cloud Gateway):
        //   JwtBearerFilter (order -90)  — HMAC-SHA256 Bearer JWT
        //   ApiKeyFilter (order -95)      — X-Api-Key header / api_key param
        //   BasicAuthGatewayFilter (order -85) — HTTP Basic credentials
        // These GlobalFilter beans must NOT be passed to addFilterBefore() because
        // that method expects WebFilter; Spring Cloud Gateway discovers them automatically.

        // OAuth2 resource-server (external IdP): only active when explicitly enabled.
        // Bearer/Basic/ApiKey auth is enforced by the GlobalFilter beans directly.
        if (authProps.isEnabled() && authProps.getOauth2().isEnabled()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwkSetUri(authProps.getOauth2().getJwkSetUri())));
        }

        return http.build();
    }
}
