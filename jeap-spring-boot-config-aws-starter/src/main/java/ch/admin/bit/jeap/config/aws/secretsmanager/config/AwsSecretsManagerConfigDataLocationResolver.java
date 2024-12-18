package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import ch.admin.bit.jeap.config.aws.context.ConfigContexts;
import ch.admin.bit.jeap.config.aws.secretsmanager.JeapAwsSecretsManagerProperties;
import org.springframework.boot.context.config.*;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Collections;
import java.util.List;

/**
 * Notice: This class is based on code from the Spring Cloud AWS project, which is licensed under the Apache License 2.0
 * and available at <a href="https://github.com/awspring/spring-cloud-aws">github.com/awspring/spring-cloud-aws</a>.
 */
public class AwsSecretsManagerConfigDataLocationResolver implements ConfigDataLocationResolver<AwsSecretsManagerConfigDataResource> {

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location.hasPrefix(AwsSecretsManagerConfigDataLoader.AWS_SECRETSMANAGER);
    }

    @Override
    public List<AwsSecretsManagerConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
        String locations = location.getNonPrefixedValue(AwsSecretsManagerConfigDataLoader.AWS_SECRETSMANAGER);
        List<String> secretNames = locations == null ? Collections.emptyList() : List.of(locations.split(";"));
        JeapAwsSecretsManagerProperties props = context.getBootstrapContext()
                .getOrElseSupply(JeapAwsSecretsManagerProperties.class, () -> loadJeapProperties(context.getBinder()));

        boolean enabled = isSecretsManagerIntegrationEnabled(props, context);

        if (enabled && !context.getBootstrapContext().isRegistered(SecretsManagerClient.class)) {
            SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder()
                    .endpointOverride(props.getEndpointOverrideUri())
                    .region(props.getRegionOverride())
                    .credentialsProvider(props.getCredentialsProvider())
                    .build();
            ConfigContexts.registerAndPromoteBean(context, SecretsManagerClient.class, ctx -> secretsManagerClient);
        }

        List<AwsSecretsManagerConfigDataResource> resources = secretNames.stream()
                .filter(secretName -> !secretName.isEmpty())
                .map(secretName -> resolve(context, location, secretName, enabled))
                .toList();

        if (!location.isOptional() && resources.isEmpty()) {
            throw new ConfigDataLocationNotFoundException(location);
        }

        return resources;
    }

    private AwsSecretsManagerConfigDataResource resolve(ConfigDataLocationResolverContext context,
                                                        ConfigDataLocation location,
                                                        String secretName, boolean enabled) {
        AwsSecretsManagerPropertySources propertySources = new AwsSecretsManagerPropertySources();
        return new AwsSecretsManagerConfigDataResource(
                secretName,
                location.isOptional(),
                enabled,
                propertySources);
    }

    private boolean isSecretsManagerIntegrationEnabled(JeapAwsSecretsManagerProperties props, ConfigDataLocationResolverContext context) {
        return props.isEnabled() && loadLegacyProperties(context.getBinder()).isEnabled();
    }

    protected JeapAwsSecretsManagerProperties loadJeapProperties(Binder binder) {
        return binder.bind(JeapAwsSecretsManagerProperties.CONFIG_PREFIX, Bindable.of(JeapAwsSecretsManagerProperties.class))
                .orElseGet(JeapAwsSecretsManagerProperties::new);
    }

    protected SpringCloudSecretsManagerProperties loadLegacyProperties(Binder binder) {
        return binder.bind(SpringCloudSecretsManagerProperties.CONFIG_PREFIX, Bindable.of(SpringCloudSecretsManagerProperties.class))
                .orElseGet(SpringCloudSecretsManagerProperties::new);
    }
}
