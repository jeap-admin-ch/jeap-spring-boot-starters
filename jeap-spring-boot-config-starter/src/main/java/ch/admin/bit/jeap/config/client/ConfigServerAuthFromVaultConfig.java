package ch.admin.bit.jeap.config.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServiceBootstrapConfiguration;
import org.springframework.cloud.vault.config.VaultBootstrapPropertySourceConfiguration;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

@Slf4j
@Conditional(ConfigServerAuthFromVaultConfig.ActivationCondition.class)
@AutoConfigureAfter(VaultBootstrapPropertySourceConfiguration.class)
@AutoConfigureBefore(ConfigServiceBootstrapConfiguration.class)
@SuppressWarnings({"ConstantConditions", "deprecation"})
@Configuration
public class ConfigServerAuthFromVaultConfig {

    private static final String USERNAME_PROPERTY = "jeap.config.client.vault.config-server.username.property";
    private static final String PASSWORD_PROPERTY = "jeap.config.client.vault.config-server.password.property";

    public static String AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME = "configServerAuth";

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ConfigServerAuthFromVaultConfig(PropertySourceLocator vaultPropertySourceLocator, ConfigurableEnvironment environment, ConfigClientProperties configClientProperties) {
        log.info("Accessing vault to fetch the config server authentication credentials.");
        PropertySource<?> vaultSource = vaultPropertySourceLocator.locate(environment);
        String usernameProperty = environment.getProperty(USERNAME_PROPERTY);
        String passwordProperty = environment.getProperty(PASSWORD_PROPERTY);
        log.debug("Using the properties '{}' and '{}' to fetch the config server authentication credentials from vault.",
                usernameProperty, passwordProperty);
        String configServerUsername = (String) vaultSource.getProperty(usernameProperty);
        String configServerPassword = (String) vaultSource.getProperty(passwordProperty);
        validateCredentials(usernameProperty, configServerUsername, passwordProperty, configServerPassword);
        Map<String, Object> configServerAuthPropertiesMap = Map.of(
                "spring.cloud.config.username", configServerUsername,
                "spring.cloud.config.password", configServerPassword
        );
        MapPropertySource configServerAuthProperties =
                new MapPropertySource(AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME, configServerAuthPropertiesMap);
        log.debug("Adding config server authentication credentials fetched from vault to the application environment.");
        environment.getPropertySources().addFirst(configServerAuthProperties);
        log.debug("Setting config server authentication credentials fetched from vault in the ConfigClientProperties.");
        configClientProperties.setUsername(configServerUsername);
        configClientProperties.setPassword(configServerPassword);
    }

    private void validateCredentials(String usernameProperty, String configServerUsername,
                                     String passwordProperty, String configServerPassword) {
        if (configServerUsername == null || configServerUsername.isBlank()) {
            throw new IllegalArgumentException(
                    String.format("Config server username has not been set in vault property %s.", usernameProperty));
        }
        if (configServerPassword == null || configServerPassword.isBlank()) {
            throw new IllegalArgumentException(
                    String.format("Config server password has not been set in vault property %s.", passwordProperty));
        }
    }

    @Slf4j
    static class ActivationCondition implements Condition {
        @SuppressWarnings("NullableProblems")
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            log.info("Checking if the config server authentication credentials should be fetched from vault.");
            Environment environment = context.getEnvironment();
            String usernameProperty = environment.getProperty(USERNAME_PROPERTY);
            if ((usernameProperty == null) || (usernameProperty.isBlank())) {
                log.info("Username property {} not set, won't configure config server credentials from vault.", USERNAME_PROPERTY);
                return false;
            }
            String passwordProperty = environment.getProperty(PASSWORD_PROPERTY);
            if ((passwordProperty == null) || (passwordProperty.isBlank())) {
                log.info("Password property {} not set, won't configure config server credentials from vault.", PASSWORD_PROPERTY);
                return false;
            }
            log.info("Will configure config server credentials from vault.");
            return true;
        }
    }

}
