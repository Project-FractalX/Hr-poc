package org.fractalx.generated.employeeservice;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import io.opentelemetry.api.common.Attributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry SDK configuration for this service.
 *
 * <p>This bean is a fallback — if Spring Boot's Micrometer Tracing auto-configuration
 * already provides an {@link OpenTelemetry} bean (e.g. via
 * {@code management.otlp.tracing.endpoint}), this config is skipped.
 * Otherwise, spans are exported via OTLP/gRPC to the configured endpoint.
 *
 * <p>Configure the endpoint with:
 * <pre>
 * management:
 *   otlp:
 *     tracing:
 *       endpoint: http://localhost:4318/v1/traces
 * </pre>
 * or via environment variable {@code OTEL_EXPORTER_OTLP_ENDPOINT}.
 */
@Configuration
public class OtelConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Fallback OTEL SDK bean — only created when Spring Boot auto-config has not
     * already registered an {@link OpenTelemetry} instance (e.g. when
     * {@code management.otlp.tracing.endpoint} is not set).
     */
    @Bean
    @ConditionalOnMissingBean(OpenTelemetry.class)
    public OpenTelemetry openTelemetry(
            @Value("${fractalx.observability.otel.endpoint:http://localhost:4317}") String endpoint) {

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName)));

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                W3CBaggagePropagator.getInstance())))
                .buildAndRegisterGlobal();
    }
}
