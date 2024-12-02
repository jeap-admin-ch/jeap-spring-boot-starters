package ch.admin.bit.jeap.monitor.metrics.dependency;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;


@Slf4j
class DependencyVersionProvider {
    private final static String AUTOMATIC_MODULE_MANIFEST_ATTRIBUTE = "Automatic-Module-Name";
    private final static String IMPLEMENTATION_TITLE_MANIFEST_ATTRIBUTE = "Implementation-Title";
    private final static String IMPLEMENTATION_VERSION_MANIFEST_ATTRIBUTE = "Implementation-Version";
    /**
     * Matches /.../spring-xxx.jar!... and /.../jeap-xxx.jar!...
     */
    private static final Pattern SPRING_AND_JEAP_JARS_PATTERN = Pattern.compile(".*((/spring-boot|/jeap-).+?\\.jar!).*");

    private final ClassLoader classLoader;

    DependencyVersionProvider() {
        classLoader = getClass().getClassLoader();
    }

    /**
     * Allows to use a specific class loader for tests
     */
    DependencyVersionProvider(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Map<String, String> getJeapAndSpringDependencyVersions() throws IOException {
        Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
        List<URL> urls = Collections.list(resources);
        return urls.stream()
                .filter(DependencyVersionProvider::isJeapOrSpringJar)
                .map(DependencyVersionProvider::getNameAndVersionFromManifest)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(Version::name, Version::version, this::mergeVersions));
    }

    static boolean isJeapOrSpringJar(URL url) {
        return SPRING_AND_JEAP_JARS_PATTERN.matcher(url.toString()).matches();
    }

    private static Optional<Version> getNameAndVersionFromManifest(URL url) {
        try (InputStream is = url.openConnection().getInputStream()) {
            return getNameAndVersionFromManifest(is);
        } catch (IOException e) {
            log.warn("Could not open resource {} on classpath", url, e);
            return Optional.empty();
        }
    }

    private static Optional<Version> getNameAndVersionFromManifest(InputStream manifestStream) throws IOException {
        Manifest manifest = new Manifest(manifestStream);
        String name = manifest.getMainAttributes().getValue(AUTOMATIC_MODULE_MANIFEST_ATTRIBUTE);
        if (name == null) {
            name = manifest.getMainAttributes().getValue(IMPLEMENTATION_TITLE_MANIFEST_ATTRIBUTE);
            if (name == null) {
                return Optional.empty();
            }
        }
        String version = manifest.getMainAttributes().getValue(IMPLEMENTATION_VERSION_MANIFEST_ATTRIBUTE);
        if (version == null) {
            return Optional.empty();
        }
        log.debug("Found dependency {} in version {} on classpath", name, version);
        return Optional.of(new Version(name, version));
    }

    /**
     * If dependencies appear on the classpath multiple times with the same name,currently the first entry
     * is used, which reflects the behaviour of the java classloader.
     */
    private String mergeVersions(String left, String right) {
        return left;
    }

    private record Version(String name, String version) {
    }
}
