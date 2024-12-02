package ch.admin.bit.jeap.config.aws.appconfig.refresh;

import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataChangedListener;
import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient;
import ch.admin.bit.jeap.config.aws.appconfig.config.AppConfigPropertySource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;


@Slf4j
@RequiredArgsConstructor
public class AppConfigContextRefresher implements JeapAppConfigDataChangedListener {

    private final ConfigurableEnvironment environment;
    private final ContextRefresher refresher;

    @PostConstruct
    void registerAsDataChangedListener() {
        // Register this context refresher as the change listener with all config property sources backed by a JeapAppConfigDataClient.
        environment.getPropertySources().stream()
                .filter(propertySource -> propertySource instanceof AppConfigPropertySource)
                .map(propertySource -> (AppConfigPropertySource) propertySource)
                .filter(appConfigPropertySource -> appConfigPropertySource.getSource() instanceof JeapAppConfigDataClient)
                .map(appConfigPropertySource -> (JeapAppConfigDataClient) appConfigPropertySource.getSource())
                .forEach(jeapAppConfigDataClient -> jeapAppConfigDataClient.setConfigDataChangedListener(this));
    }

    @Override
    public void appConfigDataChanged(String profileId) {
        log.info("Starting refresh of spring boot application context to reflect a new configuration from app config profile '{}'.", profileId);
        refresher.refresh();
        log.info("Spring boot application context refresh for app config profile '{}' completed.", profileId);
    }

}
