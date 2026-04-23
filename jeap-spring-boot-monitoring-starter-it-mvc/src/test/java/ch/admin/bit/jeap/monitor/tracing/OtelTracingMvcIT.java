package ch.admin.bit.jeap.monitor.tracing;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test that proves the OpenTelemetry tracing pipeline works end to
 * end when {@code jeap-spring-boot-monitoring-starter} is applied to an MVC
 * application:
 * <ul>
 *     <li>an inbound HTTP request produces a server span;</li>
 *     <li>that span reaches the configured {@code SpanExporter} (OTLP in
 *         production, {@link InMemorySpanExporter} here);</li>
 *     <li>a W3C {@code traceparent} header sent by the caller is adopted as
 *         the parent of the server span (propagation);</li>
 *     <li>a B3 multi-header trace context is also accepted as parent
 *         (migration-phase interoperability).</li>
 * </ul>
 */
@AutoConfigureObservability
@SpringBootTest(
        classes = OtelTracingMvcIT.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.tracing.sampling.probability=1.0",
                "spring.application.name=otel-tracing-it",
                "server.servlet.context-path=/"
        })
@Import(OtelTracingMvcIT.TestExporterConfig.class)
class OtelTracingMvcIT {

    private static final String TRACE_ID = "4bf92f3577b57614aaf2e1e3a3b7b5e0";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";

    @SpringBootApplication
    static class TestApp {
    }

    @RestController
    static class HelloController {
        @GetMapping("/hello")
        String hello() {
            return "hi";
        }
    }

    @TestConfiguration
    static class TestExporterConfig {
        @Bean
        HelloController helloController() {
            return new HelloController();
        }

        @Bean
        @Primary
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }

        @Bean
        SpanProcessor testSpanProcessor(SpanExporter exporter) {
            return SimpleSpanProcessor.create(exporter);
        }

        // Expose /hello publicly so the tracing assertions can observe a server
        // span without authenticating.
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain helloSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/hello")
                .authorizeHttpRequests(r -> r.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    InMemorySpanExporter spanExporter;

    @BeforeEach
    void clearExporter() {
        spanExporter.reset();
    }

    @Test
    void incomingRequestProducesServerSpanAndReachesTheExporter() {
        Response response = RestAssured.given().port(port).get("/hello");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().asString()).isEqualTo("hi");

        SpanData serverSpan = waitForServerSpan();
        assertThat(serverSpan.getTraceId())
                .as("OpenTelemetry emits 128-bit (32-hex) trace ids")
                .hasSize(32)
                .matches("[0-9a-f]{32}");
    }

    @Test
    void w3cTraceparentHeaderIsAdoptedAsParent() {
        String traceparent = "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";

        RestAssured.given()
                .port(port)
                .header("traceparent", traceparent)
                .get("/hello")
                .then().statusCode(200);

        SpanData serverSpan = waitForServerSpan();
        assertThat(serverSpan.getTraceId())
                .as("inbound W3C trace id must be adopted by the server span")
                .isEqualTo(TRACE_ID);
        assertThat(serverSpan.getParentSpanId())
                .as("inbound W3C parent span id must become the server span's parent")
                .isEqualTo(PARENT_SPAN_ID);
    }

    @Test
    void b3MultiHeaderTraceContextIsAccepted() {
        // B3 multi-header may still be emitted by jEAP services that have not
        // yet moved off Brave. The propagator defaults ship
        // consume=[W3C,B3,B3_MULTI] so that correlation survives mixed-version fleets.
        RestAssured.given()
                .port(port)
                .header("X-B3-TraceId", TRACE_ID)
                .header("X-B3-SpanId", PARENT_SPAN_ID)
                .header("X-B3-Sampled", "1")
                .get("/hello")
                .then().statusCode(200);

        SpanData serverSpan = waitForServerSpan();
        assertThat(serverSpan.getTraceId())
                .as("inbound B3 trace id must be adopted by the server span")
                .isEqualTo(TRACE_ID);
        assertThat(serverSpan.getParentSpanId())
                .as("inbound B3 parent span id must become the server span's parent")
                .isEqualTo(PARENT_SPAN_ID);
    }

    @Test
    void b3SingleHeaderTraceContextIsAccepted() {
        // B3 single-header format (traceId-spanId-sampled) is the other B3
        // variant enabled by the propagator defaults (consume=[W3C,B3,B3_MULTI]).
        String b3 = TRACE_ID + "-" + PARENT_SPAN_ID + "-1";

        RestAssured.given()
                .port(port)
                .header("b3", b3)
                .get("/hello")
                .then().statusCode(200);

        SpanData serverSpan = waitForServerSpan();
        assertThat(serverSpan.getTraceId())
                .as("inbound B3 single-header trace id must be adopted by the server span")
                .isEqualTo(TRACE_ID);
        assertThat(serverSpan.getParentSpanId())
                .as("inbound B3 single-header parent span id must become the server span's parent")
                .isEqualTo(PARENT_SPAN_ID);
    }

    private SpanData waitForServerSpan() {
        await().atMost(java.time.Duration.ofSeconds(5))
                .until(() -> spanExporter.getFinishedSpanItems().stream()
                        .anyMatch(s -> "SERVER".equals(s.getKind().name())));
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        return spans.stream()
                .filter(s -> "SERVER".equals(s.getKind().name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no SERVER span exported, got: " + spans));
    }
}
