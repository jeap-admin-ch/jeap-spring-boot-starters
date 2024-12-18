package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import ch.admin.bit.jeap.config.aws.context.BootstrapLoggingHelper;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Collections;
import java.util.Map;

/**
 * Notice: This class is based on code from the Spring Cloud AWS project, which is licensed under the Apache License 2.0
 * and available at <a href="https://github.com/awspring/spring-cloud-aws">github.com/awspring/spring-cloud-aws</a>.
 */
public class AwsSecretsManagerConfigDataLoader implements ConfigDataLoader<AwsSecretsManagerConfigDataResource> {

    static final String AWS_SECRETSMANAGER = "aws-secretsmanager:";

    public AwsSecretsManagerConfigDataLoader(DeferredLogFactory logFactory) {
        BootstrapLoggingHelper.reconfigureLoggers(logFactory,
                "ch.admin.bit.jeap.config.aws.appconfig.config.AppConfigPropertySources",
                "ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient");
    }

    @Override
    public boolean isLoadable(ConfigDataLoaderContext context, AwsSecretsManagerConfigDataResource resource) {
        return ConfigDataLoader.super.isLoadable(context, resource);
    }

    @Override
    public ConfigData load(ConfigDataLoaderContext context, AwsSecretsManagerConfigDataResource resource) throws ConfigDataResourceNotFoundException {
        try {
            if (resource.isEnabled()) {
                SecretsManagerClient client = context.getBootstrapContext().get(SecretsManagerClient.class);

                AwsSecretsManagerPropertySource propertySource = resource.getPropertySources()
                        .createPropertySource(resource, client);
                if (propertySource != null) {
                    return new ConfigData(Collections.singletonList(propertySource));
                } else {
                    return null;
                }
            } else {
                // create dummy empty config data
                return new ConfigData(
                        Collections.singletonList(new MapPropertySource(AWS_SECRETSMANAGER + context, Map.of())));
            }
        } catch (Exception e) {
            throw new ConfigDataResourceNotFoundException(resource, e);
        }
    }
}
