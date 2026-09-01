package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoConfiguration
@Conditional(JeapTokenIntrospectionEnabled.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
                                    !authServerConfig.getIntrospection().isIntrospectionDeactivated())
                        .map(this::toJeapTokenIntrospectorConfiguration)
                        .collect(Collectors.toMap(JeapTokenIntrospectorConfiguration::issuer, factory::create));
        return new JeapJwtIntrospector(issuerTokenIntrospectors);
    }

    @Bean
    @Conditional(JeapIntrospectionModeNotCustom.class)
    JeapJwtIntrospectionCondition jeapJwtIntrospectionCondition(ResourceServerProperties resourceServerProperties) {
        return switch (resourceServerProperties.getIntrospection().getMode()) {
            case ALWAYS -> new AlwaysTokenIntrospectionCondition();
            case LIGHTWEIGHT -> new LightweightTokenIntrospectionCondition();
            default -> new NeverTokenIntrospectionCondition();
        };
    }

    @Bean
    JeapJwtIntrospection jeapJwtIntrospection(JeapJwtIntrospector jwtIntrospector,
                                              ObjectProvider<JeapJwtIntrospectionCondition> introspectionConditions,
                                              ResourceServerProperties resourceServerProperties,
                                              Optional<JeapTokenIntrospectionMetrics> jeapTokenIntrospectionMetrics) {
        JeapJwtIntrospectionCondition introspectionCondition = selectIntrospectionCondition(
                introspectionConditions.stream().toList(), resourceServerProperties.getIntrospection().getMode());
        return new JeapJwtIntrospection(jwtIntrospector, introspectionCondition, jeapTokenIntrospectionMetrics);
    }

    /**
     * The introspection mode determines the introspection condition to use: mode CUSTOM requires exactly one condition
     * bean provided by the application, every other mode uses the built-in condition bean registered by this
     * configuration (see {@link #jeapJwtIntrospectionCondition(ResourceServerProperties)}) - a custom condition bean
     * is rejected then, as it would not be used.
     */
    private static JeapJwtIntrospectionCondition selectIntrospectionCondition(List<JeapJwtIntrospectionCondition> introspectionConditions,
                                                                              IntrospectionMode introspectionMode) {
        if (introspectionMode == IntrospectionMode.CUSTOM) {
            if (introspectionConditions.size() != 1) {
                throw new IllegalStateException(("Introspection mode CUSTOM requires exactly one JeapJwtIntrospectionCondition bean " +
                        "to be provided by the application, but found %d.").formatted(introspectionConditions.size()));
            }
        } else if (introspectionConditions.isEmpty()) { // the built-in condition should always be there
            throw new IllegalStateException("Token introspection mode is not set to CUSTOM, but the built-in " +
                    "JeapJwtIntrospectionCondition bean is missing. This indicates a bug in the jEAP token " +
                    "introspection auto-configuration.");
        } else if (introspectionConditions.size() > 1) { // only the built-in condition expected
            throw new IllegalStateException("Token introspection mode is not set to CUSTOM, yet at least one superfluous " +
                    "custom JeapJwtIntrospectionCondition bean seems to be present.");
        }
        return introspectionConditions.getFirst();
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
