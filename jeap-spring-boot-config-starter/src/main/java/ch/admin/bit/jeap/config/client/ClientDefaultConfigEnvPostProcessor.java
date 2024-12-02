package ch.admin.bit.jeap.config.client;

import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

@Order // defaults to lowest -> run after other property sources have been added so those can overwrite the properties of this source.
public class ClientDefaultConfigEnvPostProcessor extends AbstractClientConfigEnvPostProcessor {

    protected ClientDefaultConfigEnvPostProcessor(DeferredLogFactory logFactory) {
        super(logFactory);
    }

    public static String CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME = "client-config-enabled-default";

    public static final Map<String, Object> ENABLED_DEFAULT_PROPERTIES = Map.of(
            "spring.cloud.config.uri", "${vcap.services.config.credentials.uri}",
            "spring.cloud.config.username", "${vcap.services.config.credentials.username}",
            "spring.cloud.config.password", "${vcap.services.config.credentials.password}");

    @Override
    protected void doPostProcessEnvironment(ConfigurableEnvironment environment, boolean configClientEnabled) {
        if (configClientEnabled) {
            log.info("Config client is enabled -> adding missing config client default properties.");
            addMissingPropertiesAsPropertySource(environment, ENABLED_DEFAULT_PROPERTIES, CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME);
        } else {
            log.info("Config client is disabled -> not adding default properties.");
        }
    }

}
