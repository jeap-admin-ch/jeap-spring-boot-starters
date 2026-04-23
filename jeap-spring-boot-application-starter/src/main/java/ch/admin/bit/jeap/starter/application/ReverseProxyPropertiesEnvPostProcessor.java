package ch.admin.bit.jeap.starter.application;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class ReverseProxyPropertiesEnvPostProcessor implements EnvironmentPostProcessor {

    /**
     * <p>
     * This env post processor defaults tomcat/spring to use relative redirects by default. This avoids issues
     * with protocol downgrades when tomcat redirects the context root without trailing slash, and reduces the
     * risk of forwarding-header confusion when using a chain with multiple reverse proxies.
     * It also defaults server.forward-headers-strategy to NATIVE, as jEAP applications are usually deployed
     * behind a reverse proxy that sets standard forwarding headers.
     * </p>
     * <p>
     * See <a href="https://confluence.bit.admin.ch/display/JEAP/Redirects+und+Reverse+Proxies">Redirects und Reverse Proxies</a>
     * and <a href="https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.use-behind-a-proxy-server">
     * Running Behind a Front-end Proxy Server</a>.
     * </p>
     * <pre>
     * server:
     *   forward-headers-strategy: NATIVE
     *   tomcat:
     *     use-relative-redirects: true
     * </pre>
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        Map<String, Object> map = Map.of(
                "server.tomcat.use-relative-redirects", "true",
                // jEAP applications are usually operated on a cloud platform behind a reverse proxy
                // that sets standard forwarding headers. Using NATIVE strategy makes Spring
                // use these headers to reconstruct the original request URL.
                // Note that Spring activates the NATIVE strategy automatically when
                // "server.forward-headers-strategy" is not set and it detects a cloud platform
                // like Cloud Foundry or Heroku. However, jEAP applications may also be deployed
                // on other platforms (e.g., AWS ECS) where this auto-detection does not work before Spring Boot 4.
                // See TomcatWebServerFactoryCustomizer
                "server.forward-headers-strategy", "NATIVE"
        );
        environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
    }
}
