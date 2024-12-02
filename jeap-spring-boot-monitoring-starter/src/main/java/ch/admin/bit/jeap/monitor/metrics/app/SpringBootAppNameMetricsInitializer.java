package ch.admin.bit.jeap.monitor.metrics.app;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
// The meter registry api expects generic wildcard types for its arguments
@SuppressWarnings("java:S1452")
public class SpringBootAppNameMetricsInitializer {

    private static final String SPRING_APP_METRIC_NAME = "jeap_spring_app";
    private static final String TAG_NAME = "name";

    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name:na}")
    private String applicationName = "na";

    @PostConstruct
    void initialize() {
        MultiGauge.builder(SPRING_APP_METRIC_NAME)
                .description("Spring App Info")
                .register(meterRegistry)
                .register(List.of(createSpringBootAppNameRow()));
    }

    private MultiGauge.Row<Number> createSpringBootAppNameRow() {
        Tag tag = Tag.of(TAG_NAME, applicationName);

        return MultiGauge.Row.of(Tags.of(tag), 1);
    }
}
