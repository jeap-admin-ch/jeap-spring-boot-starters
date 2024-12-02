package ch.admin.bit.jeap.featureflag.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.spi.FeatureProvider;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(MeterRegistry.class)
// The meter registry api expects generic wildcard types for its arguments
@SuppressWarnings("java:S1452")
public class FeatureFlagsMetricsConfig {

    private static final String METRIC_NAME = "feature_flag";

    private final Optional<MeterRegistry> meterRegistryOptional; // only register metrics if provided
    private final FeatureProvider featureProvider;
    private final StateRepository stateRepository;

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    void initialize() {
        meterRegistryOptional.ifPresent( meterRegistry -> {
            Set<Feature> featureSet = featureProvider.getFeatures();

            List<FeatureFlagMetric> featureFlagMetricList = featureSet.stream()
                    .map(feature -> new FeatureFlagMetric(applicationName, feature))
                    .collect(toList());

            List<MultiGauge.Row<?>> featureFlagMetric = createFeatureFlagMetric(featureFlagMetricList);
            MultiGauge.builder(METRIC_NAME)
                    .description("Feature Flags")
                    .register(meterRegistry)
                    .register(featureFlagMetric);
        });
    }

    List<MultiGauge.Row<?>> createFeatureFlagMetric(List<FeatureFlagMetric> featureFlagMetricList) {
        return featureFlagMetricList.stream()
                .map(this::row)
                .collect(toList());
    }

    private MultiGauge.Row<?> row(FeatureFlagMetric featureFlagMetric) {
        Tags tags = tags(featureFlagMetric);
        return MultiGauge.Row.of(tags, () -> valueFunction(featureFlagMetric));
    }

    private Number valueFunction(FeatureFlagMetric featureFlagMetric) {
        Feature feature = featureFlagMetric.feature();
        FeatureState featureState = stateRepository.getFeatureState(feature);
        if (featureState.isEnabled()) {
            return 1;
        } else {
            return 0;
        }
    }

    private static Tags tags(FeatureFlagMetric featureFlagMetric) {
        return Tags.of(
                Tag.of("name", featureFlagMetric.feature().name()),
                Tag.of("client", featureFlagMetric.client())
        );
    }

}
