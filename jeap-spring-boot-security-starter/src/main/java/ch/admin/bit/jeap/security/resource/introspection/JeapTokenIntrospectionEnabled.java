package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.IntrospectionMode;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches if the application has been configured with a resource server introspection mode that activates introspection.
 */
public class JeapTokenIntrospectionEnabled implements Condition {

    static final String RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY = "jeap.security.oauth2.resourceserver.introspection.mode";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return Binder.get(conditionContext.getEnvironment())
                .bind(RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY, IntrospectionMode.class)
                .map(IntrospectionMode::doesActivateIntrospection)
                .orElseGet(() -> false);
    }

}
