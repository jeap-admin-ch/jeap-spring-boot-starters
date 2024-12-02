package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

abstract class AbstractClientConfigEnvPostProcessor implements EnvironmentPostProcessor {

    protected Log log;

    protected AbstractClientConfigEnvPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean configClientEnabled =  Boolean.parseBoolean(
                environment.getProperty("jeap.config.client.enabled", "true"));
        doPostProcessEnvironment(environment, configClientEnabled);
    }
    abstract protected void doPostProcessEnvironment(ConfigurableEnvironment environment, boolean configClientEnabled);

    protected void addMissingPropertiesAsPropertySource(ConfigurableEnvironment environment, Map<String, Object> properties, String name) {
        Map<String, Object> missingDefaults = new HashMap<>(properties);
        properties.keySet().stream().filter(environment::containsProperty).forEach(missingDefaults::remove);
        if (!missingDefaults.isEmpty()) {
            log.info("Adding missing config properties (%s) in config source %s.".formatted(
                    String.join(", ", missingDefaults.keySet()), name));
            environment.getPropertySources().addLast(new MapPropertySource(name, missingDefaults));
        } else {
            log.info("Not adding any config properties.");
        }
    }

}
