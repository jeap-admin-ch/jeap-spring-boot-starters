package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static ch.admin.bit.jeap.security.resource.introspection.JeapTokenIntrospectionEnabled.RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY;
import static ch.admin.bit.jeap.security.resource.introspection.LightweightTokenIntrospectionCondition.ROLES_PRUNED_CHARS_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the auto-configuration of the token introspection cache: the default cache factory backs off in favor of a
 * cache factory bean provided by the application, and the cache configuration is validated on startup.
 */
class JeapTokenIntrospectionCacheConfigurationTest {

    private static final String ISSUER = "https://keycloak/auth/realm";
    private static final String CACHE_ENABLED_PROPERTY = "jeap.security.oauth2.resourceserver.authorization-server.introspection.cache.enabled";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + ISSUER,
                    "jeap.security.oauth2.resourceserver.authorization-server.introspection.client-secret=test-secret",
                    CACHE_ENABLED_PROPERTY + "=true")
            .withUserConfiguration(PropertiesConfiguration.class, TestIntrospectorConfiguration.class)
            // Registered as auto-configuration, i.e. processed after the user configurations, so that the
            // @ConditionalOnMissingBean conditions of the introspection configuration see the beans of the application.
            .withConfiguration(AutoConfigurations.of(MvcJeapIntrospectionConfigurationUnconditional.class));

    @Test
    void startup_whenNoCustomCacheFactory_thenDefaultCaffeineCacheFactoryUsed() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=lightweight")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(JeapTokenIntrospectionCacheFactory.class)
                            .isInstanceOf(CaffeineJeapTokenIntrospectionCacheFactory.class);
                });
    }

    @Test
    void startup_whenCustomCacheFactoryBean_thenDefaultBacksOffAndCustomCacheUsed() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=lightweight")
                .withUserConfiguration(CustomCacheFactoryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(JeapTokenIntrospectionCacheFactory.class).isInstanceOf(RecordingCacheFactory.class);
                    RecordingCacheFactory cacheFactory = context.getBean(RecordingCacheFactory.class);
                    assertThat(cacheFactory.configuration.issuer()).isEqualTo(ISSUER);
                    assertThat(cacheFactory.configuration.maximumSize()).isEqualTo(1000);

                    context.getBean(JeapJwtIntrospection.class).introspectIfNeeded(createLightweightJwt());
                    assertThat(cacheFactory.lookups).hasValue(1);
                });
    }

    @Test
    void startup_whenCacheEnabledWithModeAlways_thenStartsWithCache() {
        // Caching is orthogonal to the introspection mode: it is up to the application to decide whether the
        // introspection responses may be cached, whatever the mode.
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=always")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JeapTokenIntrospectionCacheFactory.class);
                });
    }

    @Test
    void startup_whenCacheEnabledWithInvalidMaximumSize_thenFailsWithDescriptiveError() {
        contextRunner
                .withPropertyValues(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY + "=lightweight",
                        "jeap.security.oauth2.resourceserver.authorization-server.introspection.cache.maximum-size=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(NestedExceptionUtils.getRootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("introspection cache maximum-size must be greater than 0");
                });
    }

    private static Jwt createLightweightJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(ISSUER)
                .jti("token-id")
                .claim(ROLES_PRUNED_CHARS_CLAIM, 10000)
                .build();
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
    static class TestIntrospectorConfiguration {
        @Bean
        JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
            return _ -> _ -> Map.of("active", true);
        }
    }

    @Configuration
    static class CustomCacheFactoryConfiguration {
        @Bean
        JeapTokenIntrospectionCacheFactory customJeapTokenIntrospectionCacheFactory() {
            return new RecordingCacheFactory();
        }
    }

    /**
     * A custom cache factory creating a cache that does not cache anything but records its configuration and lookups.
     */
    private static class RecordingCacheFactory implements JeapTokenIntrospectionCacheFactory {

        private JeapTokenIntrospectionCacheConfiguration configuration;
        private final AtomicInteger lookups = new AtomicInteger();

        @Override
        public JeapTokenIntrospectionCache create(JeapTokenIntrospectionCacheConfiguration config) {
            this.configuration = config;
            return new RecordingCache();
        }

        private class RecordingCache implements JeapTokenIntrospectionCache {

            @Override
            public Map<String, Object> getOrIntrospect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection) {
                lookups.incrementAndGet();
                return introspection.get();
            }

            @Override
            public Map<String, Object> introspect(JeapTokenIntrospectionCacheKey key, Jwt jwt, Supplier<Map<String, Object>> introspection) {
                return introspection.get();
            }

        }

    }

}
