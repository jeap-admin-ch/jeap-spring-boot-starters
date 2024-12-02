package ch.admin.bit.jeap.vault.config.conditions;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that checks if the application is running on a Kubernetes platform.
 * This condition is used to conditionally enable configuration based on the environment
 * being Kubernetes.
 */
public class OnVaultAndKubernetesCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (CloudPlatform.KUBERNETES.isActive(context.getEnvironment())) {
            return ConditionOutcome.match();
        }
        return ConditionOutcome.noMatch("Condition: No Vault with Kubernetes detected.");
    }
}
