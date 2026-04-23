package ch.admin.bit.jeap.monitor.tracing;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that when {@code management.otlp.tracing.endpoint} is configured, finished
 * spans are actually pushed to that endpoint via OTLP/HTTP. Spring Boot creates the
 * {@code OtlpHttpSpanExporter} only when the endpoint property is set, so this IT
 * verifies the opt-in publishing path documented in {@code jeap-monitoring.properties}.
 */
@AutoConfigureObservability
@SpringBootTest(
        classes = OtlpTracingPublishingIT.TestApp.class,
        properties = {
                "spring.main.web-application-type=none",
                "management.tracing.sampling.probability=1.0"
        })
class OtlpTracingPublishingIT {

    @SpringBootApplication
    static class TestApp {
    }

    private static final String TRACES_PATH = "/v1/traces";

    @RegisterExtension
    static final WireMockExtension OTLP_COLLECTOR = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @DynamicPropertySource
    static void otlpEndpoint(DynamicPropertyRegistry registry) {
        registry.add("management.otlp.tracing.endpoint",
                () -> OTLP_COLLECTOR.baseUrl() + TRACES_PATH);
    }

    @Autowired
    Tracer tracer;

    @Autowired
    SdkTracerProvider sdkTracerProvider;

    @Test
    void spanIsPublishedToConfiguredOtlpEndpoint() {
        OTLP_COLLECTOR.stubFor(post(urlEqualTo(TRACES_PATH))
                .willReturn(aResponse().withStatus(200)));

        Span span = tracer.nextSpan().name("otlp-publish-test").start();
        span.end();

        // BatchSpanProcessor batches by default; force a flush so the assertion doesn't
        // have to wait out the schedule delay.
        sdkTracerProvider.forceFlush().join(5, TimeUnit.SECONDS);

        OTLP_COLLECTOR.verify(postRequestedFor(urlEqualTo(TRACES_PATH)));
        assertThat(OTLP_COLLECTOR.findAll(postRequestedFor(urlEqualTo(TRACES_PATH))))
                .hasSize(1)
                .allSatisfy(req -> assertThat(req.getBodyAsString())
                        .as("OTLP body should contain the span name (protobuf encodes strings as UTF-8)")
                        .contains("otlp-publish-test"));
    }
}
