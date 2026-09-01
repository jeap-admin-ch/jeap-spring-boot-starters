package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import static ch.admin.bit.jeap.security.resource.introspection.JeapTokenIntrospectionEnabled.RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the introspection mode alone determines the introspection condition to use: mode CUSTOM requires a
 * custom condition bean, every other mode rejects one on startup.
 */
class JeapIntrospectionConditionSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "jeap.security.oauth2.resourceserver.authorization-server.issuer=https://keycloak/auth/realm",
                    "jeap.security.oauth2.resourceserver.authorization-server.introspection.client-secret=test-secret")
            .withUserConfiguration(PropertiesConfiguration.class, MvcJeapIntrospectionConfigurationUnconditional.class);

    @Test
    void startup_whenModeNotCustomButCustomConditionBeanProvided_thenFailsWithDescriptiveError() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=always")
                .withUserConfiguration(CustomConditionConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(NestedExceptionUtils.getRootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Token introspection mode is not set to CUSTOM, yet at least one superfluous " +
                                    "custom JeapJwtIntrospectionCondition bean seems to be present.");
                });
    }

    @Test
    void startup_whenModeNotCustomAndBuiltInConditionBeanMissing_thenFailsWithDescriptiveError() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=always")
                .withUserConfiguration(RemoveBuiltInConditionBeanConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(NestedExceptionUtils.getRootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Token introspection mode is not set to CUSTOM, but the built-in " +
                                    "JeapJwtIntrospectionCondition bean is missing.");
                });
    }

    @Test
    void startup_whenModeCustomButNoCustomConditionBean_thenFailsWithDescriptiveError() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=custom")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(NestedExceptionUtils.getRootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Introspection mode CUSTOM requires exactly one JeapJwtIntrospectionCondition bean");
                });
    }

    @Test
    void startup_whenModeCustomAndOneCustomConditionBean_thenCustomConditionUsed() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=custom")
                .withUserConfiguration(CustomConditionConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(JeapJwtIntrospectionCondition.class).isInstanceOf(TestJeapJwtIntrospectionCondition.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(ResourceServerProperties.class)
    static class PropertiesConfiguration {
    }

    @Configuration
    // Removes the class-level conditions of MvcJeapIntrospectionConfiguration (servlet web application etc.) that are
    // not satisfied in this plain application context. Method-level conditions on the bean methods remain effective.
    static class MvcJeapIntrospectionConfigurationUnconditional extends MvcJeapIntrospectionConfiguration {
    }

    @Configuration
    // Simulates the built-in condition bean going missing, e.g. because of a broken condition on its bean method.
    static class RemoveBuiltInConditionBeanConfiguration {
        @Bean
        static BeanFactoryPostProcessor removeBuiltInConditionBeanDefinition() {
            return beanFactory -> ((BeanDefinitionRegistry) beanFactory).removeBeanDefinition("jeapJwtIntrospectionCondition");
        }
    }

    @Configuration
    static class CustomConditionConfiguration {
        @Bean
        JeapJwtIntrospectionCondition customJeapJwtIntrospectionCondition() {
            return new TestJeapJwtIntrospectionCondition();
        }
    }

    private static class TestJeapJwtIntrospectionCondition implements JeapJwtIntrospectionCondition {
        @Override
        public boolean needsIntrospection(Jwt jwt) {
            return true;
        }
    }

}
