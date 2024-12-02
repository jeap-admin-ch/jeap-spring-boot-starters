package ch.admin.bit.jeap.monitor.metrics.dependency;

import com.google.gson.Gson;
import io.micrometer.core.instrument.MultiGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyMetricsInitializerTest {

    private Gson gson;
    private DependencyMetricsInitializer metricsInitializer;

    @Test
    void createDependencyMetrics() throws IOException {
        List<MultiGauge.Row<?>> rows = metricsInitializer.createDependencyMetrics();

        String rowsAsJson = rows.stream().map(this::json).collect(joining());

        assertTrue(rowsAsJson.contains("{'key':'name','value':'spring.boot.starter.web'}"));
        assertTrue(rowsAsJson.contains("{'key':'version','value':'2.3.2.RELEASE'}"));
        assertTrue(rowsAsJson.contains("{'key':'name','value':'jeap-spring-boot-starter-blockchain'}"));
        assertTrue(rowsAsJson.contains("{'key':'version','value':'1.2.3'}"));
        assertEquals(2, rows.size());
    }

    @Test
    void createJavaRow() {
        MultiGauge.Row<Number> javaVersionRow = DependencyMetricsInitializer.createJavaRow();

        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersionJson = json(javaVersionRow);

        assertTrue(javaVersionJson.contains("{'key':'version','value':'" + javaVersion + "'}"));
        assertTrue(javaVersionJson.contains("{'key':'vendor','value':'" + javaVendor + "'}"));
    }

    @BeforeEach
    void beforeEach() {
        gson = new Gson();
        URL springJarUrl =
                getClass().getResource("/spring-jar-with-version-in-manifest.jar");
        URL jeapJarUrl =
                getClass().getResource("/jeap-jar-with-version-in-manifest.jar");

        ClassLoader classLoader = new URLClassLoader(new URL[]{springJarUrl, jeapJarUrl}, null);
        metricsInitializer = new DependencyMetricsInitializer(new DependencyVersionProvider(classLoader), null);
    }

    /**
     * All row attributes are private, convert to json for comparing content to avoid loads of reflection code in test
     */
    private String json(MultiGauge.Row<?> metricsRow) {
        return gson.toJson(metricsRow).replace('"', '\'');
    }
}
