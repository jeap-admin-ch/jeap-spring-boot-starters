package ch.admin.bit.jeap.starter.application;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.starter.application.ReverseProxyPropertiesEnvPostProcessor.DEPRECATED_FORWARD_HEADERS_STRATEGY;
import static ch.admin.bit.jeap.starter.application.ReverseProxyPropertiesEnvPostProcessor.FORWARDED_HEADER_STRATEGY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ReverseProxyPropertiesEnvPostProcessorTest {

    private static final String POST_PROCESSOR_SOURCE = ReverseProxyPropertiesEnvPostProcessor.class.getSimpleName();

    @Test
    void postProcessEnvironment_neitherStrategyPropertySet_defaultsToWebServer() {
        ConfigurableEnvironment environment = postProcess(environmentWith(Map.of()));

        assertThat(environment.getProperty(FORWARDED_HEADER_STRATEGY)).isEqualTo("web-server");
        assertThat(environment.containsProperty(DEPRECATED_FORWARD_HEADERS_STRATEGY)).isFalse();
        assertThat(environment.getPropertySources().stream().toList().getLast().getName())
                .as("the default has the lowest precedence")
                .isEqualTo(POST_PROCESSOR_SOURCE);
    }

    @Test
    void postProcessEnvironment_blankStrategyProperty_isTreatedAsNotSet() {
        ConfigurableEnvironment environment = postProcess(environmentWith(Map.of(DEPRECATED_FORWARD_HEADERS_STRATEGY, " ")));

        assertThat(environment.getPropertySources().contains(POST_PROCESSOR_SOURCE)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "server.request.header.forwarded.strategy",
            "server.forward-headers-strategy",
            "server.forwardHeadersStrategy"})
    void postProcessEnvironment_strategyPropertySetByApplication_addsNoDefault(String propertyName) {
        ConfigurableEnvironment environment = postProcess(environmentWith(Map.of(propertyName, "none")));

        assertThat(environment.getPropertySources().contains(POST_PROCESSOR_SOURCE)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SERVER_REQUEST_HEADER_FORWARDED_STRATEGY",
            "SERVER_FORWARDHEADERSSTRATEGY",
            "SERVER_FORWARD_HEADERS_STRATEGY"})
    void postProcessEnvironment_strategySetAsEnvironmentVariable_addsNoDefault(String variableName) {
        ConfigurableEnvironment environment = postProcess(environmentWithVariable(variableName, "framework"));

        assertThat(environment.getPropertySources().contains(POST_PROCESSOR_SOURCE)).isFalse();
    }

    @Test
    void postProcessEnvironment_doesNotDefaultRelativeRedirects() {
        ConfigurableEnvironment environment = postProcess(environmentWith(Map.of()));

        assertThat(environment.containsProperty("server.tomcat.use-relative-redirects")).isFalse();
        assertThat(environment.containsProperty("server.tomcat.servlet.use-relative-redirects")).isFalse();
    }

    static Stream<Arguments> applicationConfigurations() {
        return Stream.of(
                Arguments.of("no strategy", new String[]{}, Map.of(), "https"),
                Arguments.of("new property", new String[]{"--server.request.header.forwarded.strategy=none"}, Map.of(), "http"),
                Arguments.of("deprecated property", new String[]{"--server.forward-headers-strategy=framework"}, Map.of(), "https"),
                Arguments.of("deprecated property NONE", new String[]{"--server.forward-headers-strategy=NONE"}, Map.of(), "http"),
                Arguments.of("new property as env var", new String[]{}, Map.of("SERVER_REQUEST_HEADER_FORWARDED_STRATEGY", "none"), "http"),
                Arguments.of("deprecated property as env var", new String[]{}, Map.of("SERVER_FORWARDHEADERSSTRATEGY", "framework"), "https"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("applicationConfigurations")
    void applicationStarts_withStrategyConfiguration(String description, String[] args, Map<String, Object> environmentVariables,
                                                     String expectedScheme) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> systemEnvironment = new HashMap<>(environment.getSystemEnvironment());
        systemEnvironment.putAll(environmentVariables);
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, systemEnvironment));

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApp.class)
                .environment(environment)
                .run(withRandomPort(args))) {
            int port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);

            request(port)
                    .header("X-Forwarded-Proto", "https")
                    .get("/scheme")
                    .then().assertThat()
                    .statusCode(200)
                    .body(equalTo(expectedScheme));
            request(port)
                    .get("/redirectMe")
                    .then().assertThat()
                    .statusCode(302)
                    .header("Location", "/target");
        }
    }

    private static String[] withRandomPort(String[] args) {
        return Stream.concat(Stream.of("--server.port=0"), Stream.of(args)).toArray(String[]::new);
    }

    private static RequestSpecification request(int port) {
        return RestAssured.given()
                .redirects().follow(false)
                .port(port);
    }

    private static ConfigurableEnvironment environmentWith(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application", properties));
        return environment;
    }

    private static ConfigurableEnvironment environmentWithVariable(String name, String value) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(name, value)));
        return environment;
    }

    private static ConfigurableEnvironment postProcess(ConfigurableEnvironment environment) {
        // SpringApplication attaches the configuration property sources before running the post processors
        ConfigurationPropertySources.attach(environment);
        new ReverseProxyPropertiesEnvPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        return environment;
    }

    /**
     * Imports its controller explicitly instead of component scanning, as scanning outside of a
     * {@code @SpringBootTest} would also pick up the test applications of the other tests in this package.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(TestApp.TestController.class)
    static class TestApp {
        @RestController
        static class TestController {
            @GetMapping("redirectMe")
            public ModelAndView redirect() {
                return new ModelAndView("redirect:/target");
            }

            @GetMapping("scheme")
            public String scheme(HttpServletRequest request) {
                return request.getScheme();
            }
        }
    }
}
