package ch.admin.bit.jeap.monitor.metrics.log;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureObservability
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class LoggingMetricsContextRefreshAutoConfigTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ContextRefresher contextRefresher;

    @Test
    void metricsEventListener() {
        // Given
        Counter infoLevelMeter = (Counter) meterRegistry.getMeters().stream()
                .filter(meter -> "logback.events".equals(meter.getId().getName()))
                .filter(meter -> "info".equals(meter.getId().getTag("level")))
                .findFirst().orElseThrow();

        // When: Logging some statements before refreshing the context
        log.info("Some log statement");
        double count1 = infoLevelMeter.count();
        log.info("Some log statement");
        double count2 = infoLevelMeter.count();

        // When: Refreshing the context
        contextRefresher.refresh();

        // When: Logging some statements after refreshing the context
        log.info("Some log statement");
        double count3 = infoLevelMeter.count();

        // Then: Expect log statement counters to be increased in all cases, and for the logback metrics filter to be active
        assertThat(count2)
                .describedAs("Counter incremented before context refresh")
                .isGreaterThan(count1);
        assertThat(count3)
                .describedAs("Counter incremented after context refresh")
                .isGreaterThan(count2);
        assertThat(logbackMetricsFilterPresent())
                .describedAs("Logback metrics filter active after context refresh")
                .isTrue();
    }

    private boolean logbackMetricsFilterPresent() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        return loggerContext.getTurboFilterList().stream()
                .anyMatch(filter -> "MetricsTurboFilter".equals(filter.getClass().getSimpleName()));

    }
}
