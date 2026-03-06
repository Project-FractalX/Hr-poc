package org.fractalx.generated.recruitmentservice;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Tags the active Micrometer/OTel span with the current request's correlation ID.
 *
 * <p>The tag key "correlation.id" matches what the admin service uses when querying
 * Jaeger ({@code /api/traces?tags=correlation.id%3D<value>}), so correlation ID
 * search in the admin UI traces tab will work out of the box.
 *
 * <p>This interceptor runs in {@code preHandle()} which is guaranteed to execute
 * after Spring Boot's {@code ServerHttpObservationFilter} has already started the
 * span. The {@link Tracer#currentSpan()} call is therefore always safe here.
 */
@Configuration
public class CorrelationTracingConfig implements WebMvcConfigurer {

    private final Tracer tracer;

    public CorrelationTracingConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull Object handler) {
                String correlationId = MDC.get("correlationId");
                if (correlationId != null && !correlationId.isBlank()) {
                    io.micrometer.tracing.Span span = tracer.currentSpan();
                    if (span != null) {
                        span.tag("correlation.id", correlationId);
                    }
                }
                return true;
            }
        });
    }
}
