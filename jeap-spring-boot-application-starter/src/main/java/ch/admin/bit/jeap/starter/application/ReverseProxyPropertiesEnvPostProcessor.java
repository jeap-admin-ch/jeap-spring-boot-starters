package ch.admin.bit.jeap.starter.application;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * <p>
 * Defaults the forwarded header strategy to {@code web-server}, as jEAP applications are usually deployed
 * behind a reverse proxy that performs TLS termination and sets the standard forwarding headers. The web
 * server (Tomcat) then uses these headers to reconstruct the original request URL (scheme, host, port).
 * </p>
 * <p>
 * Spring Boot deduces the strategy from the cloud platform when it is not set, but jEAP applications also
 * run on platforms that are not detected. The default is only applied if the application sets neither
 * {@code server.request.header.forwarded.strategy} nor the deprecated {@code server.forward-headers-strategy}
 * in any property source: Spring Boot refuses to start if both properties have a value, regardless of which
 * property source wins, so jEAP must not add one next to an application's own setting.
 * </p>
 * <p>
 * Relative redirects are not configured here: {@code server.tomcat.servlet.use-relative-redirects} defaults
 * to {@code true} in Spring Boot, which avoids protocol downgrades when Tomcat redirects the context root.
 * </p>
 * <p>
 * See <a href="https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.use-behind-a-proxy-server">
 * Running Behind a Front-end Proxy Server</a>.
 * </p>
 */
public class ReverseProxyPropertiesEnvPostProcessor implements EnvironmentPostProcessor {

    static final String FORWARDED_HEADER_STRATEGY = "server.request.header.forwarded.strategy";
    static final String DEPRECATED_FORWARD_HEADERS_STRATEGY = "server.forward-headers-strategy";
    static final String DEFAULT_FORWARDED_HEADER_STRATEGY = "web-server";

    /**
     * Runs with the default (lowest) order, i.e. after the config data (application.yml, profiles, config
     * imports) has been loaded, so the application's own setting of either property is visible here.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        Binder binder = Binder.get(environment);
        if (hasValue(binder, FORWARDED_HEADER_STRATEGY) || hasValue(binder, DEPRECATED_FORWARD_HEADERS_STRATEGY)) {
            return;
        }
        Map<String, Object> map = Map.of(FORWARDED_HEADER_STRATEGY, DEFAULT_FORWARDED_HEADER_STRATEGY);
        environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
    }

    /**
     * Mirrors how Spring Boot decides whether a strategy property is set: it binds the property (relaxed
     * names, so environment variables such as {@code SERVER_FORWARDHEADERSSTRATEGY} count as well) and
     * ignores a blank value.
     */
    private static boolean hasValue(Binder binder, String propertyName) {
        return binder.bind(propertyName, String.class)
                .map(value -> StringUtils.hasText(value) ? value : null)
                .isBound();
    }
}
