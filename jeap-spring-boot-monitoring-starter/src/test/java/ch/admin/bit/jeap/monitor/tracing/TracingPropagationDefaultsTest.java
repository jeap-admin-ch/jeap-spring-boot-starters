package ch.admin.bit.jeap.monitor.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the propagation defaults shipped by
 * {@code jeap-monitoring.properties} take effect: outbound calls emit both
 * W3C ({@code traceparent}) and B3 ({@code b3}) trace headers while any
 * known format is accepted inbound. This keeps correlation working while
 * jEAP applications migrate from Brave/B3 to OpenTelemetry/W3C.
 */
@AutoConfigureObservability
@SpringBootTest(
        classes = TracingPropagationDefaultsTest.TestApp.class,
        properties = {
                "spring.main.web-application-type=none",
                "management.tracing.sampling.probability=1.0"
        })
class TracingPropagationDefaultsTest {

    @SpringBootApplication
    static class TestApp {
    }

    @Autowired
    Tracer tracer;

    @Autowired
    Propagator propagator;


    @Test
    @SuppressWarnings("DataFlowIssue")
    void outboundInjectionEmitsBothW3cAndB3Headers() {
        Map<String, String> carrier = new HashMap<>();
        Span span = tracer.nextSpan().name("outbound").start();
        try (var ignored = tracer.withSpan(span)) {
            propagator.inject(span.context(), carrier, Map::put);
        } finally {
            span.end();
        }

        assertThat(carrier)
                .as("W3C traceparent must be emitted")
                .containsKey("traceparent");
        assertThat(carrier)
                .as("B3 single-header must be emitted")
                .containsKey("b3");
    }

    @Test
    void propagatorAdvertisesAcceptanceOfAllConfiguredFormats() {
        assertThat(propagator.fields())
                .contains("traceparent")    // W3C
                .contains("b3")             // B3 single
                .contains("X-B3-TraceId");  // B3 multi
    }
}
