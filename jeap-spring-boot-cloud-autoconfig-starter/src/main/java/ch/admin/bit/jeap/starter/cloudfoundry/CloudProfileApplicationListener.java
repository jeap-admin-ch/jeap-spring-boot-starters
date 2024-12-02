package ch.admin.bit.jeap.starter.cloudfoundry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Actives the spring profile 'cloud' if the env var VCAP_APPLICATION is present, meaning that the app is running
 * on Cloud Foundry.
 * Re-implements / replaces <a href="https://github.com/cloudfoundry/java-buildpack-auto-reconfiguration/blob/main/src/main/java/org/cloudfoundry/reconfiguration/CloudProfileApplicationListener.java">CloudProfileApplicationListener.java</a>
 */
public final class CloudProfileApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProfileApplicationListener.class);
    private static final String CLOUD_PROFILE = "cloud";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;  // Before Boot properties file load to enable support for `application-cloud.properties`
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (isRunningOnCloudFoundry(environment)) {
            activateCloudProfile(environment);
        }
    }

    private void activateCloudProfile(ConfigurableEnvironment environment) {
        environment.addActiveProfile(CLOUD_PROFILE);
        LOGGER.info("Profile {} activated", CLOUD_PROFILE);
    }

    private boolean isRunningOnCloudFoundry(Environment environment) {
        return CloudPlatform.CLOUD_FOUNDRY.isActive(environment);
    }
}
