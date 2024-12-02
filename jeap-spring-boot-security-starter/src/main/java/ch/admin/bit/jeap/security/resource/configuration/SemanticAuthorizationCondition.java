package ch.admin.bit.jeap.security.resource.configuration;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Matches if the application has been configured with semantic application role authorization.
 */
public class SemanticAuthorizationCondition implements Condition {

    private static final String RESOURCE_SERVER_SYSTEM_NAME_PROPERTY = "jeap.security.oauth2.resourceserver.system-name";

    public static boolean isSemanticAuthorizationEnabled(Environment environment) {
        return StringUtils.hasText(environment.getProperty(RESOURCE_SERVER_SYSTEM_NAME_PROPERTY));
    }

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        Environment env = conditionContext.getEnvironment();
        return isSemanticAuthorizationEnabled(env);
    }

}
