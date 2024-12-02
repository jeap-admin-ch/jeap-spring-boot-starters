package ch.admin.bit.jeap.web.configuration;

import ch.admin.bit.jeap.web.configuration.servlet.ServletHeaders;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AutoConfiguration
@ConfigurationProperties(prefix = "jeap.web.headers")
@PropertySource("classpath:jeap-web-header-defaults.properties")
@Data
public class HeaderConfiguration {
    private Set<String> additionalContentSources;
    private Set<String> skipPathPrefixes = Set.of("/api");
    private Set<String> skipPathSuffixes = Set.of("-api");
    private Set<String> acceptPathPrefixes = Set.of();
    private Pattern acceptPathPattern = null;
    private Pattern skipPathPattern = null;
    /**
     * Content-Security-Policy header value. If not set, see {@link ServletHeaders}
     * for the default value used.
     */
    private String contentSecurityPolicy = null;
    private final Set<String> httpMethods = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name());

    /**
     * If no accept patterns are set, defaults to add security headers (secure by default).
     *
     * @return true if the path matches an accept Prefix or pattern, and does not match any skip prefix or pattern.
     */
    public boolean accept(String path) {
        return shouldAddHeadersForPath(path) && !shouldSkipForPath(path);
    }

    private boolean shouldAddHeadersForPath(String path) {
        boolean hasPrefixConfigured = !acceptPathPrefixes.isEmpty();
        boolean hasPatternConfigured = acceptPathPattern != null;

        if (hasPrefixConfigured) {
            boolean acceptByPrefix = acceptPathPrefixes.stream().anyMatch(prefix -> matchesPrefix(path, prefix));
            if (acceptByPrefix) {
                return true;
            }
        }
        if (hasPatternConfigured) {
            boolean acceptByPattern = matchesPattern(path, acceptPathPattern);
            if (acceptByPattern) {
                return true;
            }
        }

        // If either a prefix or a pattern are configured, one of them must haved matched above
        // Otherwise default to accept, i.e. default to add security headers (secure by default)
        return !hasPatternConfigured && !hasPrefixConfigured;
    }

    /**
     * @return true if the path matches skipPathPrefixes or skipPathPattern
     */
    private boolean shouldSkipForPath(String path) {
        boolean skipPath = false;
        if (!skipPathPrefixes.isEmpty()) {
            skipPath = skipPathPrefixes.stream().anyMatch(prefix -> matchesPrefix(path, prefix));
        }
        if (!skipPath && !skipPathSuffixes.isEmpty()) {
            skipPath = skipPathSuffixes.stream().anyMatch(suffix -> matchesSuffix(path, suffix));
        }

        return skipPath || matchesPattern(path, skipPathPattern);
    }

    private boolean matchesPrefix(String path, String prefix) {
        return prefix != null && path.startsWith(prefix);
    }

    private boolean matchesSuffix(String path, String suffix) {
        if (suffix != null && path.startsWith("/")) {
            String[] pathSegments = path.split("/");
            if (pathSegments.length >= 1) {
                return pathSegments[1].endsWith(suffix);
            }
        }
        return false;
    }

    private boolean matchesPattern(String path, Pattern pattern) {
        return pattern != null && pattern.matcher(path).matches();
    }

    public Set<String> getAdditionalContentSources() {
        return additionalContentSources == null ? Set.of() : additionalContentSources.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
