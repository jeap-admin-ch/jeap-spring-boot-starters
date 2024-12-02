package ch.admin.bit.jeap.vault;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class VaultDefaultPropertiesEnvPostProcessor extends AbstractVaultPropertiesEnvPostProcessor implements Ordered {

    private static final String LOCATION_DEFAULT_PROPERTIES = "classpath:/jeap-vault-starter-default.properties";

    protected VaultDefaultPropertiesEnvPostProcessor(DeferredLogFactory logFactory) {
        super(logFactory);
    }

    @Override
    protected void doPostProcessEnvironment(ConfigurableEnvironment environment) {
        if (!CloudPlatform.KUBERNETES.isActive(environment)) {
            log.info("Vault is enabled -> adding missing vault default properties.");
            addPropertySourceFromResource(environment, LOCATION_DEFAULT_PROPERTIES);
        } else {
            log.info("Vault for RHOS is enabled --> not adding any vault default properties.");
        }
    }

    @Override
    public int getOrder() {
        // Run after other property sources have been added to allow those to overwrite the properties of this source.
        return Ordered.LOWEST_PRECEDENCE;
    }

}