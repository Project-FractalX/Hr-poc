package org.fractalx.generated.sagaorchestrator;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
