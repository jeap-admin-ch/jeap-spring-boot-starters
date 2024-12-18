package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientConfigEnvPostProcessorsTest {

    private MockEnvironment mockEnvironment;
    private ClientBaseConfigEnvPostProcessor baseEnvPostProcessor;
    private ClientDefaultConfigEnvPostProcessor defaultEnvPostProcessor;
    private final DeferredLogFactory deferredLogFactory = mock(DeferredLogFactory.class);


    @BeforeEach
    void initialize() {
        when(deferredLogFactory.getLog((Class<?>) any())).thenAnswer(invocation ->
                LogFactory.getLog(invocation.getArgument(0).getClass()));
        mockEnvironment = new MockEnvironment();
        baseEnvPostProcessor = new ClientBaseConfigEnvPostProcessor(deferredLogFactory);
        defaultEnvPostProcessor = new ClientDefaultConfigEnvPostProcessor(deferredLogFactory);
    }

    private void runPostProcessors(MockEnvironment mockEnvironment) {
        baseEnvPostProcessor.postProcessEnvironment(mockEnvironment, null);
        defaultEnvPostProcessor.postProcessEnvironment(mockEnvironment, null);
    }

    @Test
    void test_WhenEnabledNotSetInBootStrapContext_ThenBaseAndDefaultsForEnabledSet() {
        addBootstrapPropertySource(mockEnvironment);
        addInterpolatedPropertyValues(mockEnvironment, ClientBaseConfigEnvPostProcessor.ENABLED_BASE_PROPERTIES);
        addInterpolatedPropertyValues(mockEnvironment, ClientDefaultConfigEnvPostProcessor.ENABLED_DEFAULT_PROPERTIES);

        runPostProcessors(mockEnvironment);

        assertProperties(mockEnvironment, ClientBaseConfigEnvPostProcessor.ENABLED_BASE_PROPERTIES);
        assertProperties(mockEnvironment, ClientDefaultConfigEnvPostProcessor.ENABLED_DEFAULT_PROPERTIES);
    }

    @Test
    void test_WhenEnabledNotSetNotInBootStrapContext_ThenBaseAndDefaultsForEnabledSet() {
        addInterpolatedPropertyValues(mockEnvironment, ClientBaseConfigEnvPostProcessor.ENABLED_BASE_PROPERTIES);
        addInterpolatedPropertyValues(mockEnvironment, ClientDefaultConfigEnvPostProcessor.ENABLED_DEFAULT_PROPERTIES);

        runPostProcessors(mockEnvironment);

        assertProperties(mockEnvironment, ClientBaseConfigEnvPostProcessor.ENABLED_BASE_PROPERTIES);
        assertProperties(mockEnvironment, ClientDefaultConfigEnvPostProcessor.ENABLED_DEFAULT_PROPERTIES);
    }


    @Test
    void test_WhenEnabledTrueInBootStrapContext_ThenBaseAndDefaultsForEnabledSet() {
        mockEnvironment.withProperty("jeap.config.client.enabled", "true");
        test_WhenEnabledNotSetInBootStrapContext_ThenBaseAndDefaultsForEnabledSet();
    }

    @Test
    void test_WhenEnabledTrueNotInBootStrapContext_ThenBaseForEnabledSetAndNoDefaultsSet() {
        mockEnvironment.withProperty("jeap.config.client.enabled", "true");
        test_WhenEnabledNotSetNotInBootStrapContext_ThenBaseAndDefaultsForEnabledSet();
    }

    @Test
    void test_WhenEnabledFalse_ThenDisabledSet() {
        mockEnvironment.withProperty("jeap.config.client.enabled", "false");
        addInterpolatedPropertyValues(mockEnvironment, ClientBaseConfigEnvPostProcessor.DISABLED_PROPERTIES);

        runPostProcessors(mockEnvironment);

        assertProperties(mockEnvironment, ClientBaseConfigEnvPostProcessor.DISABLED_PROPERTIES);
    }

    @Test
    void test_WhenEnabledInBootStrapContextAndSomePropertiesSet_ThenDefaultsForEnabledOverridenByPropertiesSet() {
        addBootstrapPropertySource(mockEnvironment);
        Map<String, Object> expectedDefaultProperties = new HashMap<>(ClientDefaultConfigEnvPostProcessor.ENABLED_DEFAULT_PROPERTIES);
        mockEnvironment.withProperty("spring.cloud.config.uri", "overridden-uri");
        expectedDefaultProperties.remove("spring.cloud.config.uri");
        mockEnvironment.withProperty("spring.cloud.config.fail-fast", "overridden-fail-fast");
        expectedDefaultProperties.remove("spring.cloud.config.fail-fast");
        addInterpolatedPropertyValues(mockEnvironment, expectedDefaultProperties);

        runPostProcessors(mockEnvironment);

        assertThat(mockEnvironment.getProperty("spring.cloud.config.uri")).isEqualTo("overridden-uri");
        assertThat(mockEnvironment.getProperty("spring.cloud.config.fail-fast")).isEqualTo("overridden-fail-fast");
        assertProperties(mockEnvironment, expectedDefaultProperties);
    }

    @Test
    void testGetInterpolatedValue() {
        assertThat(getInterpolatedValue("foobar")).isEqualTo("foobar");
        assertThat(getInterpolatedValue("foo.bar")).isEqualTo("foo.bar");
        assertThat(getInterpolatedValue("${foo.bar")).isEqualTo("${foo.bar");
        assertThat(getInterpolatedValue("foo.bar}")).isEqualTo("foo.bar}");
        assertThat(getInterpolatedValue("${foo.bar}")).isEqualTo("bar");
    }

    private static void assertProperties(Environment environment, Map<String, Object> properties) {
        properties.forEach((key, o) -> {
            assertThat(environment.containsProperty(key)).isTrue();
            if (o instanceof String value) {
                assertThat(environment.getProperty(key)).isEqualTo(getInterpolatedValue(value));
            }
        });
    }

    private static void addInterpolatedPropertyValues(MockEnvironment mockEnvironment, Map<String, Object> properties) {
        properties.forEach((key, o) -> {
            if (o instanceof String value) {
                String interpolatedPropertyName = getInterpolatedPropertyName(value);
                if (interpolatedPropertyName != null) {
                    mockEnvironment.withProperty(interpolatedPropertyName, getInterpolatedValue(value));
                }
            }
        });
    }

    private static void addBootstrapPropertySource(MockEnvironment mockEnvironment) {
        mockEnvironment.getPropertySources().
                addLast(new MapPropertySource("bootstrap", Map.of("bootstrap.dummy", "value")));
    }

    private static String getInterpolatedValue(String value) {
        String interpolatedPropertyName = getInterpolatedPropertyName(value);
        if (interpolatedPropertyName != null) {
            String[] nameParts = interpolatedPropertyName.split("[.:]");
            return nameParts[nameParts.length - 1]; // last name part
        } else {
            return value;
        }
    }

    private static String getInterpolatedPropertyName(String property) {
        final Pattern interpolatedPropertyPattern = Pattern.compile("\\$\\{(.+)}");
        Matcher interpolatedPropertyMatcher = interpolatedPropertyPattern.matcher(property);
        if (interpolatedPropertyMatcher.matches()) {
            return interpolatedPropertyMatcher.group(1);
        } else {
            return null;
        }
    }

}
