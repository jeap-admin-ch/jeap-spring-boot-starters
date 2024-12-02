package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.JeapAWSAppConfigProperties;
import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient;
import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClientFactory;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

import java.util.List;

public class AppConfigDataLoader implements ConfigDataLoader<AppConfigDataResource> {

    public AppConfigDataLoader(DeferredLogFactory logFactory) {
        BootstrapLoggingHelper.reconfigureLoggers(logFactory,
                "ch.admin.bit.jeap.config.aws.appconfig.config.AppConfigPropertySources",
                "ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient");
    }

    @Override
    @Nullable
    @SuppressWarnings("java:S106")
    public ConfigData load(ConfigDataLoaderContext context, AppConfigDataResource resource) {
        try {
            JeapAWSAppConfigProperties props = context.getBootstrapContext().get(JeapAWSAppConfigProperties.class);
            JeapAppConfigDataClient dataClient = JeapAppConfigDataClientFactory.create(
                    resource.getAppId(),
                    props.getEnvId(),
                    resource.getProfileId(),
                    props.getRequiredMinimumPollIntervalInSeconds(),
                    context.getBootstrapContext().get(AppConfigDataClient.class));
            String propertySourceName = resource.getAppId() + "/" + resource.getProfileId();
            AppConfigPropertySource<JeapAppConfigDataClient> propertySource = resource.getPropertySources()
                    .createPropertySource(propertySourceName, resource.isOptional(), dataClient);

            if (propertySource != null) {
                return new ConfigData(List.of(propertySource));
            } else {
                return null;
            }
        } catch (Exception e) {
            //No Logger at this point available
            e.printStackTrace(System.err);
            throw new ConfigDataResourceNotFoundException(resource, e);
        }
    }

}
