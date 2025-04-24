package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.AuthorizationServerConfiguration;
import ch.admin.bit.jeap.security.resource.properties.IntrospectionProperties;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@AutoConfiguration
@Conditional(JeapTokenIntrospectionEnabled.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class MvcJeapIntrospectionConfiguration {

    @Bean
    @ConditionalOnMissingBean(JeapTokenIntrospectorFactory.class)
    JeapTokenIntrospectorFactory jeapTokenIntrospectorFactory() {
        return new DefaultJeapTokenIntrospectorFactory();
    }

    @Bean
    JeapJwtIntrospector jeapJwtIntrospector(JeapTokenIntrospectorFactory factory, ResourceServerProperties resourceServerProperties) {
        Map<String, JeapTokenIntrospector> issuerTokenIntrospectors =
                resourceServerProperties.getAllAuthServerConfigurations().stream()
                        .filter(authServerConfig -> authServerConfig.getIntrospection() != null)
                        .map(this::toJeapTokenIntrospectorConfiguration)
                        .collect(Collectors.toMap(JeapTokenIntrospectorConfiguration::issuer, factory::create));
        return new JeapJwtIntrospector(issuerTokenIntrospectors);
    }

    @Bean
    @ConditionalOnExpression("!'CUSTOM'.equalsIgnoreCase('${jeap.security.resourceserver.introspection-mode:}')")
    JeapJwtIntrospectionCondition jeapJwtIntrospectionCondition(ResourceServerProperties resourceServerProperties) {
        return switch (resourceServerProperties.getIntrospectionMode()) {
            case ALWAYS -> new AlwaysTokenIntrospectionCondition();
            case LIGHTWEIGHT -> new LightweightTokenIntrospectionCondition();
            default -> new NeverTokenIntrospectionCondition();
        };
    }

    @Bean
    JeapJwtIntrospection jeapJwtIntrospection(JeapJwtIntrospector jwtIntrospector, JeapJwtIntrospectionCondition introspectionCondition) {
        return new JeapJwtIntrospection(jwtIntrospector, introspectionCondition);
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
