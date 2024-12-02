package ch.admin.bit.jeap.starter.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class RelativeRedirectsEnvPostProcessor implements EnvironmentPostProcessor {

    /**
     * <p>
     * This env post processor defaults tomcat/spring to use relative redirects by default. This avoids issues
     * with protocol downgrades when tomcat redirects the context root without trailing slash, and reduces the
     * risk of forwarding-header confusion when using a chain with multiple reverse proxies.
     * </p>
     * <p>
     * See <a href="https://confluence.bit.admin.ch/display/JEAP/Redirects+und+Reverse+Proxies">Redirects und Reverse Proxies</a>
     * and <a href="https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.use-behind-a-proxy-server">
     * Running Behind a Front-end Proxy Server</a>.
     * </p>
     * <pre>
     * server:
     *   tomcat:
     *     use-relative-redirects: true
     * </pre>
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = Map.of(
                "server.tomcat.use-relative-redirects", "true"
        );
        environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
    }
}
