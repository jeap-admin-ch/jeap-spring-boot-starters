package ch.admin.bit.jeap.monitor.metrics.dependency;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DependencyVersionProviderTest {

    @Test
    void getJeapAndSpringDependencyVersions() throws IOException {
        URL springJarUrl =
                getClass().getResource("/spring-jar-with-version-in-manifest.jar");
        URL jeapJarUrl =
                getClass().getResource("/jeap-jar-with-version-in-manifest.jar");
        URL otherJarUrl =
                getClass().getResource("/other-jar-with-version-in-manifest.jar");

        ClassLoader classLoader = new URLClassLoader(new URL[]{springJarUrl, jeapJarUrl, otherJarUrl}, null);
        DependencyVersionProvider dependencyVersionProvider = new DependencyVersionProvider(classLoader);

        Map<String, String> deps = dependencyVersionProvider.getJeapAndSpringDependencyVersions();

        assertEquals("2.3.2.RELEASE", deps.get("spring.boot.starter.web"));
        assertEquals("1.2.3", deps.get("jeap-spring-boot-starter-blockchain"));
        assertEquals(2, deps.size());
    }

    @Test
    void isJeapOrSpringJar() throws MalformedURLException {
        URL springJarUrl = new URL(
                "jar:file:/home/dev/.m2/repository/org/springframework/boot/spring-boot/3.3.2/spring-boot-3.3.2.jar!/META-INF/MANIFEST.MF");
        URL jeapJarUrl = new URL(
                "jar:file:/home/dev/.m2/repository/ch/admin/bit/jeap/jeap-spring-boot-monitoring-starter/16.3.0-SNAPSHOT/jeap-spring-boot-monitoring-starter-16.3.0-SNAPSHOT.jar!/META-INF/MANIFEST.MF");
        URL otherJarUrl = new URL(
                "jar:file:/home/dev/.m2/repository/io/micrometer/micrometer-tracing/1.3.2/micrometer-tracing-1.3.2.jar!/META-INF/MANIFEST.MF");

        assertTrue(DependencyVersionProvider.isJeapOrSpringJar(springJarUrl));
        assertTrue(DependencyVersionProvider.isJeapOrSpringJar(jeapJarUrl));
        assertFalse(DependencyVersionProvider.isJeapOrSpringJar(otherJarUrl));
    }
}
