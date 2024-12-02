package ch.admin.bit.jeap.monitor.metrics.dependency;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
// The meter registry api expects generic wildcard types for its arguments
@SuppressWarnings("java:S1452")
class DependencyMetricsInitializer {
    private static final String METRIC_NAME = "jeap_dependency_version";
    private static final String JAVA_METRIC_NAME = "jeap_java_version";
    private static final String TAG_DEPENDENCY_NAME = "name";
    private static final String TAG_VERSION = "version";
    private static final String TAG_VM_VERSION = "vmversion";
    private static final String TAG_RUNTIME_VERSION = "runtimeversion";
    private static final String TAG_CLASS_VERSION = "classversion";
    private static final String TAG_VENDOR = "vendor";
    /**
     * While we're mostly interested in the tag, all metrics need a value... Set it to 1 allows e.g. queries counting
     * the number of dependencies or to check if dependency metrics are available at all.
     */
    private static final int DEPENDENCY_METRIC_VALUE = 1;

    private final DependencyVersionProvider dependencyVersionProvider;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void initialize() throws IOException {
        List<MultiGauge.Row<?>> versions = createDependencyMetrics();

        MultiGauge.builder(METRIC_NAME)
                .description("Dependency Versions")
                .register(meterRegistry)
                .register(versions);

        MultiGauge.builder(JAVA_METRIC_NAME)
                .description("Java Version")
                .register(meterRegistry)
                .register(List.of(createJavaRow()));
    }

    List<MultiGauge.Row<?>> createDependencyMetrics() throws IOException {
        Map<String, String> dependencyVersions = new LinkedHashMap<>(
                dependencyVersionProvider.getJeapAndSpringDependencyVersions());

        return dependencyVersions.entrySet().stream()
                .map(entry -> createRow(entry.getKey(), entry.getValue()))
                .collect(toList());
    }

    private static MultiGauge.Row<Number> createRow(String dependencyName, String version) {
        Tags tags = Tags.of(TAG_DEPENDENCY_NAME, dependencyName, TAG_VERSION, version);
        return MultiGauge.Row.of(tags, DEPENDENCY_METRIC_VALUE);
    }

    static MultiGauge.Row<Number> createJavaRow() {
        String javaVersion = System.getProperty("java.version");
        String javaVmVersion = System.getProperty("java.vm.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String javaClassVersion = System.getProperty("java.class.version");

        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of(TAG_VERSION, javaVersion));
        if (javaVmVersion != null) {
            tags.add(Tag.of(TAG_VM_VERSION, javaVmVersion));
        }
        if (javaVendor != null) {
            tags.add(Tag.of(TAG_VENDOR, javaVendor));
        }
        if (javaRuntimeVersion != null) {
            tags.add(Tag.of(TAG_RUNTIME_VERSION, javaRuntimeVersion));
        }
        if (javaClassVersion != null) {
            tags.add(Tag.of(TAG_CLASS_VERSION, javaClassVersion));
        }

        return MultiGauge.Row.of(Tags.of(tags), DEPENDENCY_METRIC_VALUE);
    }
}
