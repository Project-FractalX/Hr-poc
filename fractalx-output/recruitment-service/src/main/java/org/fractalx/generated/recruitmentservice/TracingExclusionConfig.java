package org.fractalx.generated.recruitmentservice;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Excludes noisy observations from tracing so Jaeger stays clean:
 * <ul>
 *   <li>Actuator health/metrics endpoints</li>
 *   <li>Scheduled tasks (e.g. outbox poller running every second)</li>
 * </ul>
 */
@Configuration
public class TracingExclusionConfig {

    @Bean
    public ObservationPredicate noActuatorTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext sroc) {
                return !sroc.getCarrier().getRequestURI().startsWith("/actuator");
            }
            return true;
        };
    }

    @Bean
    public ObservationPredicate noScheduledTaskTracing() {
        return (name, context) -> !name.startsWith("tasks.scheduled");
    }
}
