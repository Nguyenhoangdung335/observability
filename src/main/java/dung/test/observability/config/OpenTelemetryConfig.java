package dung.test.observability.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.time.Duration;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class OpenTelemetryConfig {

    @Value("${otel.service.name:observability}")
    private String applicationName;

    @Value("${otel.collector.endpoint}")
    private String otelCollectorEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        // Set up the resource that describes this application
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), applicationName
                )));

        // Create and register the tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(createSpanProcessor())
                .build();

        // Create and register the meter provider
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(createMetricReader())
                .build();

        // Create and register the logger provider
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(createLogProcessor())
                .build();

        // Build the OpenTelemetry instance

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        GlobalOpenTelemetry.set(sdk);

        return sdk;
    }

    private SpanProcessor createSpanProcessor() {
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otelCollectorEndpoint)
                .setTimeout(Duration.ofSeconds(30))
                .build();
        return BatchSpanProcessor.builder(exporter).build();
    }

    private MetricReader createMetricReader() {
        OtlpGrpcMetricExporter exporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(otelCollectorEndpoint)
                .build();
        return PeriodicMetricReader.builder(exporter)
                .setInterval(Duration.ofSeconds(5))
                .build();
    }

    private LogRecordProcessor createLogProcessor() {
        OtlpGrpcLogRecordExporter exporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(otelCollectorEndpoint)
                .build();
        return BatchLogRecordProcessor.builder(exporter).build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }

    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter(applicationName);
    }

    @Bean
    public Logger logger(OpenTelemetry openTelemetry) {
        return openTelemetry.getLogsBridge()
                .loggerBuilder(applicationName)
                .build();
    }
}
