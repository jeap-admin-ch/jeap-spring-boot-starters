package ch.admin.bit.jeap.config.client;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

// Run before the ConfigDataEnvironmentPostProcessor in order to provide the config client base configuration before
// it is needed by the ConfigDataEnvironmentPostProcessor to import config data from the config server.
@Order(ConfigDataEnvironmentPostProcessor.ORDER - 1)
public class ClientBaseConfigEnvPostProcessor extends AbstractClientConfigEnvPostProcessor {

    protected ClientBaseConfigEnvPostProcessor(DeferredLogFactory logFactory) {
        super(logFactory);
    }

    public static final Map<String, Object> ENABLED_BASE_PROPERTIES = Map.of(
            "spring.cloud.config.fail-fast", "${jeap.config.client.fail-fast:true}",
            "info.config.version", "${jeap.config.server.config.version}",
            "info.config.buildtime", "${jeap.config.server.config.version.buildtime}",
            "info.config.commitid", "${jeap.config.server.config.version.commitid}");

    public static final Map<String, Object> DISABLED_PROPERTIES = Map.of(
            "spring.cloud.config.enabled", "false",
            "spring.cloud.bus.enabled", "false");


    @Override
    protected void doPostProcessEnvironment(ConfigurableEnvironment environment, boolean configClientEnabled) {
        if (configClientEnabled) {
            log.info("Config client is enabled -> adding missing config client base properties.");
            addMissingPropertiesAsPropertySource(environment, ENABLED_BASE_PROPERTIES, "client-config-enabled-base");
        } else {
            log.info("Config client is disabled -> adding config client disabled properties.");
            addMissingPropertiesAsPropertySource(environment, DISABLED_PROPERTIES, "client-config-disabled");
        }
    }

}
