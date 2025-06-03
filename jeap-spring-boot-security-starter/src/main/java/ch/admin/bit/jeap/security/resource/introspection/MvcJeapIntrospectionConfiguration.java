package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoConfiguration
@Conditional(JeapTokenIntrospectionEnabled.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class MvcJeapIntrospectionConfiguration {

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    JeapTokenIntrospectionMetrics jeapTokenIntrospectionMetrics(Optional<MeterRegistry> meterRegistry) {
        return new JeapTokenIntrospectionMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(JeapTokenIntrospectorFactory.class)
    JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory(Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics) {
        return new DefaultJeapTokenIntrospectorFactory(jeapTokenIntrospectionMetrics);
    }

    @Bean
    JeapJwtIntrospector jeapJwtIntrospector(JeapTokenIntrospectorFactory factory, ResourceServerProperties resourceServerProperties) {
        Map<String, JeapTokenIntrospector> issuerTokenIntrospectors =
                resourceServerProperties.getAllAuthServerConfigurations().stream()
                        .filter(authServerConfig ->
                                authServerConfig.getIntrospection() != null &&
                                    authServerConfig.getIntrospection().getMode() != IntrospectionMode.NONE)
                        .map(this::toJeapTokenIntrospectorConfiguration)
                        .collect(Collectors.toMap(JeapTokenIntrospectorConfiguration::issuer, factory::create));
        return new JeapJwtIntrospector(issuerTokenIntrospectors);
    }

    @Bean
    @ConditionalOnExpression("!'CUSTOM'.equalsIgnoreCase('${jeap.security.resourceserver.introspection.mode:}')")
    JeapJwtIntrospectionCondition jeapJwtIntrospectionCondition(ResourceServerProperties resourceServerProperties) {
        return switch (resourceServerProperties.getIntrospection().getMode()) {
            case ALWAYS -> new AlwaysTokenIntrospectionCondition();
            case LIGHTWEIGHT -> new LightweightTokenIntrospectionCondition();
            default -> new NeverTokenIntrospectionCondition();
        };
    }

    @Bean
    JeapJwtIntrospection jeapJwtIntrospection(JeapJwtIntrospector jwtIntrospector,
                                              JeapJwtIntrospectionCondition introspectionCondition,
                                              Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics) {
        return new JeapJwtIntrospection(jwtIntrospector, introspectionCondition, jeapTokenIntrospectionMetrics);
    }

    private JeapTokenIntrospectorConfiguration toJeapTokenIntrospectorConfiguration(AuthorizationServerConfiguration authorizationServerConfiguration) {
        IntrospectionProperties introspectionProperties = authorizationServerConfiguration.getIntrospection();
        return new JeapTokenIntrospectorConfiguration(
                authorizationServerConfiguration.getIssuer(),
                introspectionProperties.getUri(),
                introspectionProperties.getClientId(),
                introspectionProperties.getClientSecret(),
                Duration.ofMillis(introspectionProperties.getConnectTimeoutInMillis()),
                Duration.ofMillis(introspectionProperties.getReadTimeoutInMillis()));
    }

}
