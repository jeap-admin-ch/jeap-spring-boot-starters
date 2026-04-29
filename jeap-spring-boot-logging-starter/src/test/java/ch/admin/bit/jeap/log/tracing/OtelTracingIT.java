package ch.admin.bit.jeap.log.tracing;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test proving that starting a span populates the SLF4J MDC with {@code traceId} / {@code spanId}, and
 * that the Logback pattern renders the tracing context.
 **/
@AutoConfigureTracing
@SpringBootTest(
        classes = OtelTracingIT.TestApp.class,
        properties = {
                "spring.main.web-application-type=none",
                "management.tracing.sampling.probability=1.0"
        })
class OtelTracingIT {

    // log pattern containing the trace-id and span-id
    private static final String LOG_PATTERN =
            "[%X{traceId:-},%X{spanId:-}] %logger{35} - %msg%n";

    @SpringBootApplication
    static class TestApp {
    }

    @Autowired
    Tracer tracer;

    @Test
    void tracerIsBackedByOpenTelemetry() {
        assertThat(tracer)
                .as("Expected the OTel stuff to have been included as a test dependency.")
                .isInstanceOf(OtelTracer.class);
    }

    @Test
    void startingASpan_populatesMdcAndLogbackPatternRendersIt() {
        Span span = tracer.nextSpan().name("mdc-bridge-test").start();
        try (var _ = tracer.withSpan(span)) {
            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");

            assertThat(traceId)
                    .as("OTel emits 128-bit (32 hex chars) trace ids.")
                    .isNotNull()
                    .hasSize(32)
                    .matches("[0-9a-f]{32}");
            assertThat(spanId)
                    .as("OTel emits 64-bit (16 hex chars) span ids.")
                    .isNotNull()
                    .hasSize(16)
                    .matches("[0-9a-f]{16}");
            assertThat(traceId).isEqualTo(span.context().traceId());
            assertThat(spanId).isEqualTo(span.context().spanId());

            String rendered = renderWithCurrentMdc("hello");
            assertThat(rendered).contains("[" + traceId + "," + spanId + "]");
            assertThat(rendered).contains("hello");
        } finally {
            span.end();
        }

        assertThat(MDC.get("traceId"))
                .as("The bridge must clean MDC up when the span scope closes so log lines after a span stay uncorrelated.")
                .isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    @SuppressWarnings("java:S125")
    void patternRendersEmptyPlaceholdersOutsideAnySpan() {
        // Outside a span, Micrometer Tracing leaves MDC unpopulated;
        // the pattern's ':-' default must render an empty slot, not 'null'.
        MDC.clear();

        String rendered = renderWithCurrentMdc("no-trace");

        assertThat(rendered).contains("[,]").doesNotContain("null");
    }

    private String renderWithCurrentMdc(String message) {
        LoggerContext context = new LoggerContext();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(LOG_PATTERN);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setLoggerName("ch.admin.bit.jeap.log.tracing.Test");
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setTimeStamp(System.currentTimeMillis());
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();
        event.setMDCPropertyMap(mdcSnapshot != null ? mdcSnapshot : Map.of());
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }
}
