package ch.admin.bit.jeap.vault.config;

import ch.admin.bit.jeap.vault.config.conditions.OnVaultAndKubernetesCondition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Conditional;

/**
 * Configuration class for logging Vault integration settings.
 * This class logs the Vault configuration used for authentication with the Kubernetes method.
 *
 * <p>It logs the Vault URL, Kubernetes role, Kubernetes path, and key vault backend used for the Vault integration.
 * The logging occurs only once during the initial bootstrapping to avoid duplicate log entries
 * during context refreshes.</p>
 *
 * <p>The logging is conditional based on {@link OnVaultAndKubernetesCondition}, meaning it will
 * only be activated if the condition defined in {@code OnVaultAndKubernetesCondition} is met.</p>
 *
 * <p>Annotated with {@code @Configuration} to indicate that this class provides configuration
 * settings, and {@code @Conditional(OnVaultAndKubernetesCondition.class)} to enable the configuration
 * conditionally.</p>
 *
 * @see OnVaultAndKubernetesCondition
 */
@AutoConfiguration
@Conditional(OnVaultAndKubernetesCondition.class)
@Slf4j
public class JeapVaultKubernetesConfigLogger {

    @Value("${spring.cloud.vault.uri}")
    private String vaultUrl;

    @Value("${jeap.vault.kubernetes.role}")
    private String kubernetesRole;

    @Value("${jeap.vault.kubernetes.kubernetes-path}")
    private String kubernetesPath;

    @Value("${jeap.vault.kv.backend}")
    private String keyVaultBackend;

    private static boolean bootstrapped = false;

    @PostConstruct
    void logVaultConfig() {
        // Avoid duplicate log entry during context refresh after bootstrapping
        if (!bootstrapped) {
            log.info("Vault integration is enabled with authentication method KUBERNETES, " +
                            "using vault at '{}' with kubernetes role '{}', kubernetes path '{}', " +
                            "and kv.backend '{}'",
                    vaultUrl, kubernetesRole, kubernetesPath, keyVaultBackend);
        }
        bootstrapped = true; //NOSONAR
    }
}
