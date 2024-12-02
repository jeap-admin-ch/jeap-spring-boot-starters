package ch.admin.bit.jeap.vault.config.conditions;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that checks if Vault is enabled and the authentication method is set to AppRole.
 * This condition is used to conditionally enable configuration based on the presence of
 * specific Vault properties in the environment.
 */
public class OnVaultAndAppRoleCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("spring.cloud.vault.enabled", "");
        String authentication = context.getEnvironment().getProperty("spring.cloud.vault.authentication", "");

        if ("true".equals(enabled) && "APPROLE".equals(authentication)) {
            return ConditionOutcome.match();
        }

        return ConditionOutcome.noMatch("Condition: No Vault with AppRole detected.");
    }
}
