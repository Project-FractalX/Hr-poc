package org.fractalx.gateway.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Excludes noisy observations from gateway tracing so Jaeger stays clean:
 * <ul>
 *   <li>Actuator health/metrics endpoints</li>
 *   <li>Scheduled tasks (defensive — gateway has none, but guard anyway)</li>
 * </ul>
 * Uses reflection to read the request path from the WebFlux
 * {@code ServerWebExchange} carrier, avoiding a direct import of
 * {@code ServerRequestObservationContext} whose package differs across
 * Spring Framework versions.
 */
@Configuration
public class GatewayTracingExclusionConfig {

    @Bean
    public ObservationPredicate noActuatorTracing() {
        return (name, context) -> {
            try {
                // carrier = ServerWebExchange
                Object exchange = context.getClass().getMethod("getCarrier").invoke(context);
                // request = ServerHttpRequest
                Object request  = exchange.getClass().getMethod("getRequest").invoke(exchange);
                // path   = RequestPath → toString() = "/actuator/health"
                String  path    = request.getClass().getMethod("getPath").invoke(request).toString();
                return !path.startsWith("/actuator");
            } catch (Exception ignored) {
                return true;
            }
        };
    }

    @Bean
    public ObservationPredicate noScheduledTaskTracing() {
        return (name, context) -> !name.startsWith("tasks.scheduled");
    }
}
