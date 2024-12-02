package ch.admin.bit.jeap.config.client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static ch.admin.bit.jeap.config.client.ClientDefaultConfigEnvPostProcessor.CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME;
import static ch.admin.bit.jeap.config.client.ConfigServerAuthFromVaultConfig.AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME;

/**
 * Note: Only applies if the Spring Cloud boostrap context is active.
 * This postprocessor prioritizes an existing property source with the config server credentials from vault over an existing
 * property source with the config server default configurations in the given environment. This postprocessor is needed
 * because the config server credentials from vault need to be added at a very special point during the initialization
 * of Spring Cloud Config, i.e. after the initialization of Vault but before the initialization of the Config Server, which
 * results in the corresponding property source being added to the environment with a low priority even when added using
 * environment.getPropertySources().addFirst().
 */
@RequiredArgsConstructor
class PrioritizeConfigServerAuthFromVaultEnvPostProcessor implements EnvironmentPostProcessor {

    private final Log log;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME)) {
            var authFromVaultPropertySource = propertySources.remove(AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME);
            if (authFromVaultPropertySource != null) {
                log.debug("Prioritizing property source %s over %s.".formatted(
                        AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME, CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME));
                propertySources.addBefore(CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME, authFromVaultPropertySource);
            }
        }
    }

}
