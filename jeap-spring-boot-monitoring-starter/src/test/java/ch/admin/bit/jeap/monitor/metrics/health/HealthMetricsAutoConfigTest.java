package ch.admin.bit.jeap.monitor.metrics.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthMetricsAutoConfigTest {

    private MeterRegistry meterRegistry;
    private DefaultHealthContributorRegistry contributorRegistry;
    private HealthEndpoint healthEndpoint;
    private HealthMetricsAutoConfig autoConfig;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        contributorRegistry = new DefaultHealthContributorRegistry();
        healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        autoConfig = new HealthMetricsAutoConfig(meterRegistry, contributorRegistry, healthEndpoint);
        ReflectionTestUtils.setField(autoConfig, "perContributorMetricsEnabled", true);
    }

    @Test
    void updatesMetricForSimpleHealthIndicator() {
        AtomicReference<Health> serviceHealth = new AtomicReference<>(Health.up().build());
        contributorRegistry.registerContributor("service", (HealthIndicator) serviceHealth::get);

        autoConfig.createHealthMetric();
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("service")).isEqualTo(1.0);

        serviceHealth.set(Health.down().build());
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("service")).isEqualTo(0.0);
    }

    @Test
    void updatesMetricForCompositeHealthContributor() {
        AtomicReference<Health> coreDataSource = new AtomicReference<>(Health.up().build());
        AtomicReference<Health> identifierRegistryDataSource = new AtomicReference<>(Health.up().build());
        AtomicReference<Health> statusRegistryDataSource = new AtomicReference<>(Health.up().build());

        CompositeHealthContributor dbComposite = CompositeHealthContributor.fromMap(Map.of(
                "coreDataSource", (HealthContributor) (HealthIndicator) coreDataSource::get,
                "identifierRegistryDataSource", (HealthContributor) (HealthIndicator) identifierRegistryDataSource::get,
                "statusRegistryDataSource", (HealthContributor) (HealthIndicator) statusRegistryDataSource::get
        ));
        contributorRegistry.registerContributor("db", dbComposite);

        autoConfig.createHealthMetric();
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("db")).isEqualTo(1.0);

        identifierRegistryDataSource.set(Health.down().build());
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("db")).isEqualTo(0.0);

        identifierRegistryDataSource.set(Health.up().build());
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("db")).isEqualTo(1.0);
    }

    @Test
    void updatesMetricForNestedCompositeHealthContributor() {
        AtomicReference<Health> nestedDataSource = new AtomicReference<>(Health.up().build());

        CompositeHealthContributor nestedComposite = CompositeHealthContributor.fromMap(Map.of(
                "nestedDataSource", (HealthContributor) (HealthIndicator) nestedDataSource::get
        ));
        CompositeHealthContributor outerComposite = CompositeHealthContributor.fromMap(Map.of(
                "inner", nestedComposite
        ));
        contributorRegistry.registerContributor("db", outerComposite);

        autoConfig.createHealthMetric();
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("db")).isEqualTo(1.0);

        nestedDataSource.set(Health.down().build());
        autoConfig.updateHealthMetrics();

        assertThat(contributorGaugeValue("db")).isEqualTo(0.0);
    }

    private double contributorGaugeValue(String component) {
        return meterRegistry.get("health_indicator_status")
                .tag("component", component)
                .gauge()
                .value();
    }
}