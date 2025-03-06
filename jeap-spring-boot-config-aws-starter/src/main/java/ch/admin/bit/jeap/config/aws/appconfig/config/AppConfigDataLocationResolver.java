package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.JeapAWSAppConfigProperties;
import ch.admin.bit.jeap.config.aws.appconfig.JeapSpringApplicationProperties;
import ch.admin.bit.jeap.config.aws.context.ConfigContexts;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.utils.AttributeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppConfigDataLocationResolver extends AbstractAppConfigDataLocationResolver<AppConfigDataResource> {

    public static final String PREFIX = "jeap-app-config-aws:";
    public static final String STANDARD_APP_CONFIG_COMMON_APPLICATION_NAME = "common";
    public static final String STANDARD_APP_CONFIG_COMMON_PLATFORM_APPLICATION_NAME = "common-platform";
    public static final String STANDARD_APP_CONFIG_COMMON_CERTS_APPLICATION_NAME = "common-certs";
    public static final String STANDARD_APP_CONFIG_PROFILE_NAME = "config";

    @Override
    protected String getPrefix() {
        return PREFIX;
    }

    @Override
    public List<AppConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
        JeapAWSAppConfigProperties jeapAWSAppConfigProperties = loadJeapAWSAppConfigProperties(resolverContext.getBinder());
        ConfigContexts.registerBean(resolverContext, JeapAWSAppConfigProperties.class, jeapAWSAppConfigProperties);
        AppConfigDataClient appConfigDataClient = createAppConfigDataClient(jeapAWSAppConfigProperties);
        ConfigContexts.registerAndPromoteBean(resolverContext, AppConfigDataClient.class, BootstrapRegistry.InstanceSupplier.of(appConfigDataClient));
        return getLocationArguments(location, resolverContext, jeapAWSAppConfigProperties).stream()
                .map(locationArg -> new AppConfigDataResource(locationArg.appId(), locationArg.profileId(), locationArg.optional() || location.isOptional(), new AppConfigPropertySources()))
                .collect(Collectors.toList());
    }

    private AppConfigDataClient createAppConfigDataClient(JeapAWSAppConfigProperties jeapAWSAppConfigProperties) {
        if (jeapAWSAppConfigProperties.isTrustAllCertificates()) {
            SdkHttpClient httpClient = UrlConnectionHttpClient.builder().buildWithDefaults(
                                           AttributeMap.builder().put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                           .build());
            return AppConfigDataClient.builder().httpClient(httpClient).build();
        } else {
            return AppConfigDataClient.create();
        }
    }

    private List<LocationArgument> getLocationArguments(ConfigDataLocation location, ConfigDataLocationResolverContext resolverContext, JeapAWSAppConfigProperties jeapAWSAppConfigProperties) {
        List<LocationArgument> locationArgs = getLocationArgumentStrings(location.getNonPrefixedValue(PREFIX)).stream()
                .map(LocationArgument::from)
                .collect(Collectors.toList());
        if (!locationArgs.isEmpty()) {
            return locationArgs;
        } else {
           // No explicit location arguments given -> use the defaults
            return getDefaultLocationArguments(loadJeapSpringApplicationProperties(resolverContext.getBinder()), jeapAWSAppConfigProperties);
        }
    }

    private List<LocationArgument> getDefaultLocationArguments(JeapSpringApplicationProperties jeapSpringApplicationProperties,
                                                               JeapAWSAppConfigProperties jeapAWSAppConfigProperties) {
        List<LocationArgument> defaultLocations = new ArrayList<>();
        if (!jeapAWSAppConfigProperties.isNoDefaultCommonConfig()) {
            defaultLocations.add(LocationArgument.mandatory(STANDARD_APP_CONFIG_COMMON_APPLICATION_NAME, STANDARD_APP_CONFIG_PROFILE_NAME));
        }
        defaultLocations.add(LocationArgument.optional(STANDARD_APP_CONFIG_COMMON_CERTS_APPLICATION_NAME, STANDARD_APP_CONFIG_PROFILE_NAME));
        if (!jeapAWSAppConfigProperties.isNoDefaultCommonPlatformConfig()) {
            defaultLocations.add(LocationArgument.mandatory(STANDARD_APP_CONFIG_COMMON_PLATFORM_APPLICATION_NAME, STANDARD_APP_CONFIG_PROFILE_NAME));
        }
        defaultLocations.add(LocationArgument.mandatory(jeapSpringApplicationProperties.getName(), STANDARD_APP_CONFIG_PROFILE_NAME));
        return defaultLocations;
    }

    private JeapAWSAppConfigProperties loadJeapAWSAppConfigProperties(Binder binder) {
        return binder.bind(JeapAWSAppConfigProperties.JEAP_AWS_CONFIG_PREFIX, Bindable.of(JeapAWSAppConfigProperties.class)).orElseGet(JeapAWSAppConfigProperties::new);
    }

    private JeapSpringApplicationProperties loadJeapSpringApplicationProperties(Binder binder) {
        return binder.bind(JeapSpringApplicationProperties.SPRING_APPLICATION_CONFIG_PREFIX, Bindable.of(JeapSpringApplicationProperties.class)).orElseGet(JeapSpringApplicationProperties::new);
    }

}
