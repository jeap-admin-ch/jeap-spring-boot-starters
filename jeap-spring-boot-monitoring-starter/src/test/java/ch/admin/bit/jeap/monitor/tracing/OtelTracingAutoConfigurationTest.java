package ch.admin.bit.jeap.monitor.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ClassUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that after the Brave → OpenTelemetry migration the Spring Boot
 * actuator's tracing stack wires up correctly in a jEAP application:
 * <ul>
 *     <li>the Micrometer {@link Tracer} is backed by OpenTelemetry;</li>
 *     <li>starting a span populates the SLF4J MDC keys {@code traceId} and
 *         {@code spanId};</li>
 *     <li>trace ids are 128-bit, span ids are 64 bit (as emitted by OpenTelemetry);</li>
 *     <li>spans flow through the configured {@link SpanExporter} — which in
 *         production would be OTLP.</li>
 * </ul>
 * The test replaces the OTLP exporter with an {@link InMemorySpanExporter} so
 * no real network traffic is attempted.
 */
// @AutoConfigureTracing activates the tracing stack in tests — without it, Spring Boot
// sets management.tracing.export.enabled=false and the OTel exporter pipeline is bypassed.
@AutoConfigureTracing
@SpringBootTest(
        classes = OtelTracingAutoConfigurationTest.TestApp.class,
        properties = {
                "spring.main.web-application-type=none",
                // Sample every span so the assertion is deterministic.
                "management.tracing.sampling.probability=1.0"
        })
@Import(OtelTracingAutoConfigurationTest.InMemoryExporterConfig.class)
class OtelTracingAutoConfigurationTest {

    @SpringBootApplication
    static class TestApp {
    }

    @TestConfiguration
    static class InMemoryExporterConfig {
        @Bean
        @Primary
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }

        @Bean
        SpanProcessor testSpanProcessor(SpanExporter exporter) {
            // SimpleSpanProcessor flushes on span end so assertions can read spans synchronously.
            return SimpleSpanProcessor.create(exporter);
        }
    }

    @Autowired
    Tracer tracer;

    @Autowired
    InMemorySpanExporter inMemorySpanExporter;

    @BeforeEach
    void resetExporter() {
        inMemorySpanExporter.reset();
    }

    @Test
    void tracerIsBackedByOpenTelemetry() {
        assertThat(tracer).isInstanceOf(OtelTracer.class);
    }

    @Test
    void braveClassesAreNotOnTheClasspath() {
        // Brave tracing is no longer needed after switching to OTel -> no brave classes should be left on the classpath
        assertThatClassIsMissing("brave.Tracer");
        assertThatClassIsMissing("brave.Tracing");
        assertThatClassIsMissing("brave.kafka.clients.KafkaTracing");
        assertThatClassIsMissing("zipkin2.reporter.Reporter");
        assertThatClassIsMissing("io.micrometer.tracing.brave.bridge.BraveTracer");
    }

    @Test
    void spanPopulatesMdcWithTraceIdAndSpanId() {
        Span span = tracer.nextSpan().name("mdc-test-span").start();
        try (var ignored = tracer.withSpan(span)) {
            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");

            assertThat(traceId)
                    .as("OTel emits 128-bit (32-hex) trace ids")
                    .isNotNull()
                    .hasSize(32)
                    .matches("[0-9a-f]{32}");
            assertThat(spanId)
                    .as("OTel emits 64-bit (16-hex) span ids")
                    .isNotNull()
                    .hasSize(16)
                    .matches("[0-9a-f]{16}");
            assertThat(traceId).isEqualTo(span.context().traceId());
            assertThat(spanId).isEqualTo(span.context().spanId());
        } finally {
            span.end();
        }

        // And once the span scope closes, MDC is cleaned up.
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    void spanIsExportedThroughTheSpanExporter() {
        Span span = tracer.nextSpan().name("export-test-span").start();
        span.end();

        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
        assertThat(spans)
                .extracting(SpanData::getName)
                .contains("export-test-span");
    }

    private static void assertThatClassIsMissing(String className) {
        assertThat(ClassUtils.isPresent(className, OtelTracingAutoConfigurationTest.class.getClassLoader()))
                .as("class %s must not be on the classpath after the OTel migration", className)
                .isFalse();
    }
}
