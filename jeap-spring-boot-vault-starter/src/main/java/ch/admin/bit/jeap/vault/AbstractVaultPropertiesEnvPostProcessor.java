package ch.admin.bit.jeap.vault;

import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

abstract class AbstractVaultPropertiesEnvPostProcessor implements EnvironmentPostProcessor {

    protected Log log;

    protected AbstractVaultPropertiesEnvPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean vaultEnabled = Boolean.parseBoolean(
                environment.getProperty("spring.cloud.vault.enabled", "true"));
        if (vaultEnabled) {
            doPostProcessEnvironment(environment);
        } else {
            log.info("Vault is disabled -> won't add any vault properties");
        }
    }

    abstract protected void doPostProcessEnvironment(ConfigurableEnvironment environment);

    /**
     * <p>
     * Add a MapPropertySource loaded from the given resource path to the given environment using lowest priority (addLast).
     * The property source will only contain properties that have not yet been set in the environment.
     * </p>
     * <p>
     * This is necessary to a) for users to be able to override properties, and b) to avoid re-setting the property
     * values to defaults when the EnvironmentPostProcessor runs a second time (for the first time during the boostrap
     * phase, and for the second time during the application context initialization phase).
     * </p>
     */
    @SuppressWarnings("ConstantConditions")
    @SneakyThrows
    protected void addPropertySourceFromResource(ConfigurableEnvironment env, String resourcePath) {
        ResourcePropertySource properties = new ResourcePropertySource(resourcePath);
        Map<String, Object> propertiesNotYetSetInEnvironment = Arrays.stream(properties.getPropertyNames())
                .filter(propertyName -> !env.containsProperty(propertyName))
                .collect(toMap(propertyName -> propertyName, properties::getProperty));
        if (!propertiesNotYetSetInEnvironment.isEmpty()) {
            log.info("Adding missing vault properties (%s) in config source %s.".formatted(
                    String.join(", ", propertiesNotYetSetInEnvironment.keySet()), properties.getName()));
            env.getPropertySources().addLast(
                    new MapPropertySource(properties.getName(), propertiesNotYetSetInEnvironment));
        } else {
            log.info("Not adding any vault properties.");
        }
    }

}
