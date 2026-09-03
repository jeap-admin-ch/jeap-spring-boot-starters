package ch.admin.bit.jeap.security.resource.introspection;

import ch.admin.bit.jeap.security.resource.properties.IntrospectionMode;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches if the configured resource server introspection mode does not delegate the introspection decision to a
 * custom introspection condition bean, i.e. if the built-in introspection condition applies.
 */
class JeapIntrospectionModeNotCustom implements Condition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return Binder.get(conditionContext.getEnvironment())
                .bind(JeapTokenIntrospectionEnabled.RESOURCE_SERVER_INTROSPECTION_MODE_PROPERTY, IntrospectionMode.class)
                .map(mode -> mode != IntrospectionMode.CUSTOM)
                .orElseGet(() -> true);
    }

}
