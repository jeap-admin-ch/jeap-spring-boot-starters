package ch.admin.bit.jeap.monitor.metrics.health;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthMetricsAutoConfigTest {

    @Mock
    private HealthContributorRegistry healthContributorRegistry;

    @Mock
    private HealthEndpoint healthEndpoint;

    private SimpleMeterRegistry meterRegistry;
    private HealthMetricsAutoConfig healthMetricsAutoConfig;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        healthMetricsAutoConfig = new HealthMetricsAutoConfig(meterRegistry, healthContributorRegistry, healthEndpoint);
    }

    @Test
    void createHealthMetric_registersHealthGauge() {
        healthMetricsAutoConfig.createHealthMetric();

        assertThat(meterRegistry.find("health").gauge()).isNotNull();
    }

    @Test
    void healthGauge_returnsOne_whenStatusIsUp() {
        healthMetricsAutoConfig.createHealthMetric();

        assertThat(meterRegistry.find("health").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void healthGauge_returnsZero_afterStatusUpdatedToDown() throws Exception {
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().down().build()));

        healthMetricsAutoConfig.updateHealthMetrics();

        assertThat(meterRegistry.find("health").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void updateHealthMetrics_fetchesStatusFromHealthEndpoint() throws Exception {
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().up().build()));

        healthMetricsAutoConfig.updateHealthMetrics();

        verify(healthEndpoint).health();
    }

    @Test
    void createHealthMetric_withPerContributorMetricsDisabled_doesNotRegisterPerContributorGauges() {
        healthMetricsAutoConfig.createHealthMetric();

        assertThat(meterRegistry.find("health_indicator_status").gauges()).isEmpty();
    }

    @Test
    void createHealthMetric_withPerContributorMetricsEnabled_registersGaugePerContributor() {
        HealthIndicator indicator = mock(HealthIndicator.class);
        enablePerContributorMetrics();
        when(healthContributorRegistry.stream())
                .thenAnswer(inv -> Stream.of(new HealthContributors.Entry("db", indicator)));

        healthMetricsAutoConfig.createHealthMetric();

        assertThat(meterRegistry.find("health_indicator_status").tag("component", "db").gauge()).isNotNull();
    }

    @Test
    void indicatorGauge_returnsOne_whenHealthIndicatorIsUp() throws Exception {
        HealthIndicator indicator = mock(HealthIndicator.class);
        enablePerContributorMetrics();
        when(healthContributorRegistry.stream())
                .thenAnswer(inv -> Stream.of(new HealthContributors.Entry("db", indicator)));
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().up().build()));
        when(indicator.health()).thenReturn(new Health.Builder().up().build());

        healthMetricsAutoConfig.updateHealthMetrics();

        assertThat(meterRegistry.find("health_indicator_status").tag("component", "db").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void indicatorGauge_returnsZero_whenHealthIndicatorIsDown() throws Exception {
        HealthIndicator indicator = mock(HealthIndicator.class);
        enablePerContributorMetrics();
        when(healthContributorRegistry.stream())
                .thenAnswer(inv -> Stream.of(new HealthContributors.Entry("cache", indicator)));
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().up().build()));
        when(indicator.health()).thenReturn(new Health.Builder().down().build());

        healthMetricsAutoConfig.updateHealthMetrics();

        assertThat(meterRegistry.find("health_indicator_status").tag("component", "cache").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void indicatorGauge_returnsZero_whenHealthIndicatorThrowsException() throws Exception {
        HealthIndicator indicator = mock(HealthIndicator.class);
        enablePerContributorMetrics();
        when(healthContributorRegistry.stream())
                .thenAnswer(inv -> Stream.of(new HealthContributors.Entry("flaky", indicator)));
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().up().build()));
        when(indicator.health()).thenThrow(new RuntimeException("health check timed out"));

        healthMetricsAutoConfig.updateHealthMetrics();

        assertThat(meterRegistry.find("health_indicator_status").tag("component", "flaky").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void updateHealthMetrics_withPerContributorMetricsEnabled_skipsNonHealthIndicatorContributors() throws Exception {
        CompositeHealthContributor compositeContributor = mock(CompositeHealthContributor.class);
        enablePerContributorMetrics();
        when(healthContributorRegistry.stream())
                .thenAnswer(inv -> Stream.of(new HealthContributors.Entry("composite", compositeContributor)));
        healthMetricsAutoConfig.createHealthMetric();
        when(healthEndpoint.health()).thenReturn(indicatedDescriptor(new Health.Builder().up().build()));

        healthMetricsAutoConfig.updateHealthMetrics();

        assertThat(meterRegistry.find("health_indicator_status").tag("component", "composite").gauge().value()).isEqualTo(1.0);
    }

    private void enablePerContributorMetrics() {
        ReflectionTestUtils.setField(healthMetricsAutoConfig, "perContributorMetricsEnabled", true);
    }

    private static HealthDescriptor indicatedDescriptor(Health health) throws Exception {
        Constructor<IndicatedHealthDescriptor> constructor = IndicatedHealthDescriptor.class.getDeclaredConstructor(Health.class);
        constructor.setAccessible(true);
        return constructor.newInstance(health);
    }

}

