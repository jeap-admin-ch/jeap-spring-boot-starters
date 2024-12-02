package ch.admin.bit.jeap.security.resource.configuration;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

/**
 * Matches if the application has been configured as a jEAP OAuth2 resource, i.e. if an authorization server issuer
 * (e.g. a keycloak instance or a B2B microgateway instance) have been configured via one of the following configuration
 * options: authorization-server, b2b-gateway, auth-servers.
 */
public class JeapOAuth2ResourceCondition implements Condition {

    private static final String AUTH_SERVER_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.authorization-server.issuer";
    private static final String B2B_GATEWAY_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.b2b-gateway.issuer";
    private static final String FIRST_AUTH_SERVERS_ITEM_ISSUER_CONFIG_PROPERTY = "jeap.security.oauth2.resourceserver.auth-servers[0].issuer";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        final Environment env = conditionContext.getEnvironment();
        return Stream.of(AUTH_SERVER_ISSUER_CONFIG_PROPERTY, B2B_GATEWAY_ISSUER_CONFIG_PROPERTY, FIRST_AUTH_SERVERS_ITEM_ISSUER_CONFIG_PROPERTY).
                map(env::getProperty).
                anyMatch(StringUtils::hasText);
    }

}
