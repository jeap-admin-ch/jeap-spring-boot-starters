package ch.admin.bit.jeap.featureflag.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.core.util.NamedFeature;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = {MeterRegistryConfig.class, FeatureFlagsMetricsConfig.class},
    properties = {"spring.application.name=feature-flags-metrics-test"}
)
class FeatureFlagsMetricsConfigTest {

    @Autowired
    private FeatureFlagsMetricsConfig featureFlagsMetricsInitializer;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private FeatureProvider featureProviderMock;

    @MockBean
    private StateRepository stateRepositoryMock;

    @BeforeEach
    void beforeEach() {
        meterRegistry.clear();
    }

    @Test
    void initializeWhenNoFeatureFlags() {
        when(featureProviderMock.getFeatures()).thenReturn(Collections.emptySet());
        featureFlagsMetricsInitializer.initialize();
        assertEquals(0, meterRegistry.getMeters().size());
    }

    @Test
    void initializeWithFeatureFlags() {
        Feature feature1 = new NamedFeature("FEATURE_FLAG_1");
        Feature feature2 = new NamedFeature("FEATURE_FLAG_2");
        Set<Feature> featureSet = Set.of(feature1, feature2);

        when(featureProviderMock.getFeatures()).thenReturn(featureSet);
        featureFlagsMetricsInitializer.initialize();

        assertEquals(2, meterRegistry.getMeters().size());
    }

    @Test
    void testIfTagsAreSetCorrect() {

        FeatureFlagMetric featureFlagMetric_1 = new FeatureFlagMetric("MicroserviceNameA", new NamedFeature("FeatureFlagA"));

        List<FeatureFlagMetric> featureFlagMetricList = List.of(featureFlagMetric_1);
        List<MultiGauge.Row<?>> rows = featureFlagsMetricsInitializer.createFeatureFlagMetric(featureFlagMetricList);

        MultiGauge.builder("METRIC_NAME")
                .description("Feature Flags")
                .register(meterRegistry)
                .register(rows);

        List<Meter> meterList = meterRegistry.getMeters();
        Meter meter = meterList.get(0);
        Meter.Id meterId = meter.getId();

        assertEquals("METRIC_NAME", meterId.getName());

        assertEquals(2, meterId.getTags().size());
        assertEquals("FeatureFlagA", meterId.getTag("name"));
        assertEquals("MicroserviceNameA", meterId.getTag("client"));

    }

    @Test
    void changeStateOfFeature() {
        Feature feature1 = new NamedFeature("FEATURE_FLAG_1");
        Set<Feature> featureSet = Set.of(feature1);

        when(featureProviderMock.getFeatures()).thenReturn(featureSet);

        featureFlagsMetricsInitializer.initialize();

        assertEquals(1, meterRegistry.getMeters().size());

        // Case1: StateRepo for FeatureFlag returns true (is enabled)
        when(stateRepositoryMock.getFeatureState(feature1)).thenReturn(new FeatureState(feature1, true));
        Gauge gauge = (Gauge) meterRegistry.getMeters().get(0);
        assertEquals(1.0, gauge.value());

        // Case 2: StateRepo for FeatureFlag returns false (is disabled)
        when(stateRepositoryMock.getFeatureState(feature1)).thenReturn(new FeatureState(feature1, false));
        assertEquals(0.0, gauge.value());
    }
}