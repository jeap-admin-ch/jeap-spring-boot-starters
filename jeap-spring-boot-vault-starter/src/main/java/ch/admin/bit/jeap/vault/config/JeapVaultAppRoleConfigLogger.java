package ch.admin.bit.jeap.vault.config;


import ch.admin.bit.jeap.vault.config.conditions.OnVaultAndAppRoleCondition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Conditional;

/**
 * Configuration class for logging Vault integration settings.
 * This class logs the Vault configuration used for authentication with the AppRole method.
 *
 * <p>It logs the Vault URL, AppRole path, and secret path used for the Vault integration.
 * The logging occurs only once during the initial bootstrapping to avoid duplicate log entries
 * during context refreshes.</p>
 *
 * <p>The logging is conditional based on {@link OnVaultAndAppRoleCondition}, meaning it will
 * only be activated if the condition defined in {@code OnVaultAndAppRoleCondition} is met.</p>
 *
 * <p>Annotated with {@code @Configuration} to indicate that this class provides configuration
 * settings, and {@code @Conditional(OnVaultAndAppRoleCondition.class)} to enable the configuration
 * conditionally.</p>
 *
 * @see OnVaultAndAppRoleCondition
 */
@AutoConfiguration
@Conditional(OnVaultAndAppRoleCondition.class)
@Slf4j
public class JeapVaultAppRoleConfigLogger {

    @Value("${spring.cloud.vault.uri}")
    private String vaultUrl;

    @Value("${spring.cloud.vault.app-role.app-role-path}")
    private String appRolePath;

    @Value("${spring.cloud.vault.kv.backend}")
    private String secretPath;

    private static boolean bootstrapped = false;

    @PostConstruct
    void logVaultConfig() {
        // Avoid duplicate log entry during context refresh after bootstrapping
        if (!bootstrapped) {
            log.info("Vault integration is enabled with authentication method APPROLE," +
                            " using vault at '{}' with app role path '{}' and secret path '{}'",
                    vaultUrl, appRolePath, secretPath);
        }
        bootstrapped = true; //NOSONAR
    }
}
