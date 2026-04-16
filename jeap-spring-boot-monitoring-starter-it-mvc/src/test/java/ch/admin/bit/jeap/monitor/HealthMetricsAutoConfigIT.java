package ch.admin.bit.jeap.monitor;

import ch.admin.bit.jeap.monitor.metrics.health.HealthMetricsAutoConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HealthMetricsAutoConfigIT.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = "jeap.health.metric.contributor-metrics.enabled=true")
class HealthMetricsAutoConfigIT {

    private static Health healthStatus = Health.up().build();
    private static Health coreDataSourceStatus = Health.up().build();
    private static Health identifierRegistryDataSourceStatus = Health.up().build();
    private static Health statusRegistryDataSourceStatus = Health.up().build();

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
        healthStatus = Health.up().build();
        coreDataSourceStatus = Health.up().build();
        identifierRegistryDataSourceStatus = Health.up().build();
        statusRegistryDataSourceStatus = Health.up().build();
        scheduledTask.getRunnable().run();

        // then: health metric is present and UP
        assertThat(gauge.value()).isEqualTo(1.0);

        // given: health is DOWN
        healthStatus = Health.down().build();
        scheduledTask.getRunnable().run();

        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    void itShouldProduceACompositeContributorMetric() {
        Gauge dbGauge = getContributorGauge("db");
        FixedRateTask scheduledTask = getHealthMetricScheduledTask();

        // given: all db contributors are UP
        coreDataSourceStatus = Health.up().build();
        identifierRegistryDataSourceStatus = Health.up().build();
        statusRegistryDataSourceStatus = Health.up().build();
        scheduledTask.getRunnable().run();

        assertThat(dbGauge.value()).isEqualTo(1.0);

        // given: one db contributor is DOWN
        identifierRegistryDataSourceStatus = Health.down().build();
        scheduledTask.getRunnable().run();

        assertThat(dbGauge.value()).isEqualTo(0.0);
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

    private Gauge getContributorGauge(String component) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getType() == Meter.Type.GAUGE
                        && meter.getId().getName().equals("health_indicator_status")
                        && component.equals(meter.getId().getTag("component")))
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

        @Bean(name = "db")
        public CompositeHealthContributor dbHealthContributor() {
            Map<String, HealthContributor> contributors = Map.of(
                    "coreDataSource", (HealthIndicator) () -> coreDataSourceStatus,
                    "identifierRegistryDataSource", (HealthIndicator) () -> identifierRegistryDataSourceStatus,
                    "statusRegistryDataSource", (HealthIndicator) () -> statusRegistryDataSourceStatus
            );
            return CompositeHealthContributor.fromMap(contributors);
        }
    }
}
