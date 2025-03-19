package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.health.HealthMetricsAutoConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HealthMetricsAutoConfigIT.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class HealthMetricsAutoConfigIT {

    private static Health healthStatus = Health.up().build();

    @LocalManagementPort
    int localManagementPort;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void itShouldProduceAHealthMetric() {
        Gauge gauge = getGauge();
        FixedRateTask scheduledTask = getHealthMetricScheduledTask();

        // given: health is UP initially
        scheduledTask.getRunnable().run();

        // then: health metric is present and UP
        assertThat(gauge.value())
                .isEqualTo(1.0);

        // given: health is DOWN
        healthStatus = Health.down().build();
        scheduledTask.getRunnable().run();

        assertThat(gauge.value())
                .isEqualTo(0.0);
    }

    private FixedRateTask getHealthMetricScheduledTask() {
        return scheduledTaskHolder.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(task -> task.toString().contains(HealthMetricsAutoConfig.class.getSimpleName()))
                .map(FixedRateTask.class::cast)
                .findFirst().orElseThrow();
    }

    private Gauge getGauge() {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getType() == Meter.Type.GAUGE &&
                        meter.getId().getName().equals("health"))
                .findFirst()
                .map(Gauge.class::cast)
                .orElseThrow();
    }

    @SpringBootApplication
    public static class TestApp {
        @Bean(name = "exampleHealthIndicator")
        public HealthIndicator exampleHealthIndicator() {
            return () -> healthStatus;
        }
    }
}
