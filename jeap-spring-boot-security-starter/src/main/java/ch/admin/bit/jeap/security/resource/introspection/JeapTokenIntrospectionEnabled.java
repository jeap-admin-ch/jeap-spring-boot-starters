package ch.admin.bit.jeap.security.resource.introspection;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches if the application has been configured with a resource server introspection mode other than "none".
 */
public class JeapTokenIntrospectionEnabled implements Condition {

    private static final String RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY = "jeap.security.oauth2.resourceserver.introspection-mode";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String introspectionMode = conditionContext.getEnvironment().getProperty(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY);
        return introspectionMode != null && !introspectionMode.equals("none");
    }

}
