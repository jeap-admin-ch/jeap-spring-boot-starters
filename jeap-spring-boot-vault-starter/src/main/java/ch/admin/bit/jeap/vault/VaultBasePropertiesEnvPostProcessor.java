package ch.admin.bit.jeap.vault;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Objects;

public class VaultBasePropertiesEnvPostProcessor extends AbstractVaultPropertiesEnvPostProcessor implements Ordered {

    private static final String LOCATION_BASE_PROPERTIES = "classpath:/jeap-vault-starter-base.properties";
    private static final String LOCATION_RHOS_BASE_PROPERTIES = "classpath:/jeap-vault-starter-rhos-base.properties";

    protected VaultBasePropertiesEnvPostProcessor(DeferredLogFactory logFactory) {
        super(logFactory);
    }

    @Override
    protected void doPostProcessEnvironment(ConfigurableEnvironment environment) {
        boolean isInitContainer = Boolean.parseBoolean(Objects.toString(environment.getSystemEnvironment().get("IS_INIT_CONTAINER_EXECUTION")));
        if (isInitContainer) {
            log.info("Application is running as database migration job container.");
        } else {
            if (CloudPlatform.KUBERNETES.isActive(environment)) {
                log.info("Vault for RHOS is enabled -> adding missing vault base properties.");
                addPropertySourceFromResource(environment, LOCATION_RHOS_BASE_PROPERTIES);
            } else {
                log.info("Vault is enabled -> adding missing vault base properties.");
                addPropertySourceFromResource(environment, LOCATION_BASE_PROPERTIES);
            }
        }
    }

    @Override
    public int getOrder() {
        // Run before ConfigDataEnvironmentPostProcessor to provide the jeap spring vault base configuration before
        // the spring config vault properties import gets processed.
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }
}
