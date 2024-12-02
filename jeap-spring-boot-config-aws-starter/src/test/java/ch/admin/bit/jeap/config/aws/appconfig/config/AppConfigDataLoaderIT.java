package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClientFactory;
import ch.admin.bit.jeap.config.aws.appconfig.config.LocationArgument.ConfigDataInvalidLocationArgumentException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.*;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataServiceClientConfiguration;
import software.amazon.awssdk.services.appconfigdata.model.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigDataLoaderIT {

    private static final String ATTRIBUTE_KEY = "key";
    private static final String ATTRIBUTE_VALUE_COMMON = "value-common-";
    private static final String ATTRIBUTE_VALUE_APP = "value-application-overrides-common";
    private static final String ATTRIBUTE_CONFIG_PROPS_KEY = "jeap.appconfig.test.configprops.property";
    private static final String ATTRIBUTE_CONFIG_PROPS_VALUE = "configprops-value";
    private static final String ATTRIBUTE_REFRESH_SCOPE_KEY = "jeap.appconfig.test.refreshscope.property";
    private static final String ATTRIBUTE_REFRESH_SCOPE_VALUE = "refreshscope-value";
    private static final String ATTRIBUTE_STANDARD_SCOPE_KEY = "jeap.appconfig.test.standardscope.property";
    private static final String ATTRIBUTE_STANDARD_SCOPE_VALUE = "standardscope-value";

    private static final String IMPORT_CONFIG_PROFILE = "import";

    @BeforeEach
    void init() {
        // Start each test with fresh JeapAppConfigDataClient instances in order to isolate the tests.
        JeapAppConfigDataClientFactory.evictAllCachedClients();
    }

    @Test
    void resolvesPropertyFromAppConfig_defaultConfigs() {
        String importConfiguration = "--spring.config.import=jeap-app-config-aws:";
        String appNameConfiguration = "--spring.application.name=app";
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());
        try (var context = runApplication(application, importConfiguration, appNameConfiguration)) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_defaultConfigsNoCommon() {
        String importConfiguration = "--spring.config.import=jeap-app-config-aws:";
        String appNameConfiguration = "--spring.application.name=app";
        String noDefaultCommonConfigConfiguration = "--jeap.config.aws.appconfig.no-default-common-config=true";
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());
        try (var context = runApplication(application, importConfiguration, appNameConfiguration, noDefaultCommonConfigConfiguration)) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_defaultConfigsNoCommonPlatform() {
        String importConfiguration = "--spring.config.import=jeap-app-config-aws:";
        String appNameConfiguration = "--spring.application.name=app";
        String noDefaultCommonPlatformConfigConfiguration = "--jeap.config.aws.appconfig.no-default-common-platform-config=true";
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());
        try (var context = runApplication(application, importConfiguration, appNameConfiguration, noDefaultCommonPlatformConfigConfiguration)) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_defaultConfigsNoCommonPlatformNoCommon() {
        String importConfiguration = "--spring.config.import=jeap-app-config-aws:";
        String appNameConfiguration = "--spring.application.name=app";
        String noDefaultCommonConfigConfiguration = "--jeap.config.aws.appconfig.no-default-common-config=true";
        String noDefaultCommonPlatformConfigConfiguration = "--jeap.config.aws.appconfig.no-default-common-platform-config=true";
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());
        try (var context = runApplication(application, importConfiguration, appNameConfiguration, noDefaultCommonConfigConfiguration, noDefaultCommonPlatformConfigConfiguration)) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_defaultConfig_importFromApplicationProperties() {
        SpringApplication application = new SpringApplication(Application.class);
        application.setAdditionalProfiles(IMPORT_CONFIG_PROFILE);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());
        try (var context = runApplication(application)) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--spring.config.import=jeap-app-config-aws:common/config,jeap-app-config-aws:app/config;common-platform/config",
            "--spring.config.import=jeap-app-config-aws:common/config;app/config;common-platform/config"})
    void resolvesPropertyFromAppConfig_applicationAfterCommon_appOverrides(String importConfiguration) {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        try (ConfigurableApplicationContext context = runApplication(application, importConfiguration )) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--spring.config.import=jeap-app-config-aws:app/config,jeap-app-config-aws:common/config;jeap-app-config-aws:common-platform/config",
            "--spring.config.import=jeap-app-config-aws:app/config;common/config;common-platform/config"})
    void resolvesPropertyFromAppConfig_commonAfterApplication_commonOverrides() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        try (ConfigurableApplicationContext context = runApplication(application,
                "--spring.config.import=jeap-app-config-aws:app/config,jeap-app-config-aws:common/config,jeap-app-config-aws:common-platform/config")) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_COMMON);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_onlyCommon() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        try (ConfigurableApplicationContext context = runApplication(application,
                "--spring.config.import=jeap-app-config-aws:common/config;common-platform/config")) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_COMMON);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isNull();
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_onlyApplication() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        try (ConfigurableApplicationContext context = runApplication(application,
                "--spring.config.import=jeap-app-config-aws:app/config")) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isEqualTo("true");
        }
    }

    @Test
    void resolvesPropertyFromAppConfig_noImport() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        try (ConfigurableApplicationContext context = runApplication(application, "")) {
            assertThat(context.getEnvironment().getProperty(ATTRIBUTE_KEY)).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.platform.common")).isNull();
            assertThat(context.getEnvironment().getProperty("jeap.test.application")).isNull();
        }
    }

    @Test
    @SneakyThrows
    void testPolling() {
        SpringApplication application = new SpringApplication(Application.class);
        AwsConfigurerClientConfigurationMock appConfigMock =
                new AwsConfigurerClientConfigurationMock(1, List.of(1, 2, 3));
        application.addBootstrapRegistryInitializer(appConfigMock);
        application.setAdditionalProfiles(IMPORT_CONFIG_PROFILE);
        try (ConfigurableApplicationContext context = runApplication(application,"--keepaliveseconds=8")) {
            int commonStartPollingCount = appConfigMock.getCommonPollCount();
            Thread.sleep(6250);
            int commonEndPollingCount = appConfigMock.getCommonPollCount();
            int numCommonPollingCounts = commonEndPollingCount - commonStartPollingCount;
            assertThat(numCommonPollingCounts).isBetween(6,7);
            assertThat(appConfigMock.getNumMissedPollIntervalsForApp()).isEqualTo(0);
            assertThat(appConfigMock.numUnexpectedSessionTokensReceived).isEqualTo(0);
        }
    }

    @Test
    @SneakyThrows
    void testKeepsOnPollingWhenGetLatestConfigurationRequestFails() {
        SpringApplication application = new SpringApplication(Application.class);
        AwsConfigurerClientConfigurationMock appConfigMock =
                new AwsConfigurerClientConfigurationMock(1, List.of(1));
        application.addBootstrapRegistryInitializer(appConfigMock);
        application.setAdditionalProfiles(IMPORT_CONFIG_PROFILE);
        try (ConfigurableApplicationContext context = runApplication(application,"--keepaliveseconds=10")) {
            int commonStartPollingCount = appConfigMock.getCommonPollCount();
            Thread.sleep(2250);
            appConfigMock.setCommonFailGetLatestConfigurationRequests(true);
            Thread.sleep(4250);
            appConfigMock.setCommonFailGetLatestConfigurationRequests(false);
            Thread.sleep(2250);
            int commonEndPollingCount = appConfigMock.getCommonPollCount();
            int commonNumSuccessfulPollingCounts = commonEndPollingCount - commonStartPollingCount;
            assertThat(commonNumSuccessfulPollingCounts).isBetween(4,5);
            assertThat(appConfigMock.getCommonNumFailedGetLatestConfigurationRequests()).isBetween(4,5);
        }
    }

    @Test
    void testRefreshSpringContextAfterAppConfigDataChanged() {
        final int appProfilePollIntervalSeconds = 1;
        SpringApplication application = new SpringApplication(Application.class);
        AwsConfigurerClientConfigurationMock appConfigMock =
                new AwsConfigurerClientConfigurationMock(1, List.of(appProfilePollIntervalSeconds));
        application.addBootstrapRegistryInitializer(appConfigMock);
        application.setAdditionalProfiles(IMPORT_CONFIG_PROFILE);
        try (ConfigurableApplicationContext context = runApplication(application,"--keepaliveseconds=5")) {
            Environment environment = context.getEnvironment();
            DummyService dummyService = context.getBean(DummyService.class);

            // Assert the initial configuration after the initial load of the app config property source
            assertInitialAppConfiguration(environment, dummyService);

            // Make sure that the application did poll for a new configuration (at least) twice
            assertAppProfilePolling(2, appProfilePollIntervalSeconds, appConfigMock);

            // Assert that the configuration is unchanged
            assertInitialAppConfiguration(environment, dummyService);

            // Deploy configuration changes
            final String newConfigPropsValue = "configprops-changed";
            final String newRefreshScopeValue = "refreshscope-changed";
            final String newStandardScopeValue = "standardscope-changed";
            appConfigMock.changeAppValue(ATTRIBUTE_CONFIG_PROPS_KEY, newConfigPropsValue);
            appConfigMock.changeAppValue(ATTRIBUTE_REFRESH_SCOPE_KEY, newRefreshScopeValue);
            appConfigMock.changeAppValue(ATTRIBUTE_STANDARD_SCOPE_KEY, newStandardScopeValue);

            // Make sure that the changed configuration was polled (at least) once in order to trigger the refresh
            assertAppProfilePolling(1, appProfilePollIntervalSeconds, appConfigMock);

            // Assert the new configuration
            assertThat(environment.getProperty(ATTRIBUTE_CONFIG_PROPS_KEY)).isEqualTo(newConfigPropsValue);
            assertThat(dummyService.getConfiPropertiesProperty()).isEqualTo(newConfigPropsValue);
            assertThat(environment.getProperty(ATTRIBUTE_REFRESH_SCOPE_KEY)).isEqualTo(newRefreshScopeValue);
            assertThat(dummyService.getRefreshScopedBeanProperty()).isEqualTo(newRefreshScopeValue);
            assertThat(environment.getProperty(ATTRIBUTE_STANDARD_SCOPE_KEY)).isEqualTo(newStandardScopeValue);
            assertThat(dummyService.getStandardScopedBeanProperty()).isEqualTo(ATTRIBUTE_STANDARD_SCOPE_VALUE);
            assertThat(environment.getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
        }
    }

    private void assertInitialAppConfiguration(Environment environment, DummyService dummyService) {
        assertThat(environment.getProperty(ATTRIBUTE_CONFIG_PROPS_KEY)).isEqualTo(ATTRIBUTE_CONFIG_PROPS_VALUE);
        assertThat(dummyService.getConfiPropertiesProperty()).isEqualTo(ATTRIBUTE_CONFIG_PROPS_VALUE);
        assertThat(environment.getProperty(ATTRIBUTE_REFRESH_SCOPE_KEY)).isEqualTo(ATTRIBUTE_REFRESH_SCOPE_VALUE);
        assertThat(dummyService.getRefreshScopedBeanProperty()).isEqualTo(ATTRIBUTE_REFRESH_SCOPE_VALUE);
        assertThat(environment.getProperty(ATTRIBUTE_STANDARD_SCOPE_KEY)).isEqualTo(ATTRIBUTE_STANDARD_SCOPE_VALUE);
        assertThat(dummyService.getStandardScopedBeanProperty()).isEqualTo(ATTRIBUTE_STANDARD_SCOPE_VALUE);
        assertThat(environment.getProperty(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE_APP);
    }

    private void assertAppProfilePolling(int atLeastPolls, int pollIntervalSeconds, AwsConfigurerClientConfigurationMock appConfigMock) {
        final int appConfigPollCountBefore = appConfigMock.getAppPollCount();
        try {
            Thread.sleep(atLeastPolls * pollIntervalSeconds * 1000L + 200);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }
        assertThat(appConfigMock.getAppPollCount() - appConfigPollCountBefore).isGreaterThanOrEqualTo(atLeastPolls);
    }

    private ConfigurableApplicationContext runApplication(SpringApplication application, String... args) {
        return application.run(Stream.concat(Arrays.stream(args), Stream.of(
            "--jeap.config.aws.appconfig.app-id=ConfigExample",
                    "--jeap.config.aws.appconfig.env-id=dev",
                    "--logging.level.ch.admin.bit.jeap.config.aws.appconfig=TRACE"))
                .toArray(String[]::new));
    }

    @Test
    void resolvesPropertyFromAppConfig_throwsExceptionIfLocationNotFound() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        assertThrows(ConfigDataResourceNotFoundException.class, () ->
                runApplication(application, "--spring.config.import=jeap-app-config-aws:failure/config"));
    }

    @Test
    void resolvesPropertyFromAppConfig_ignoresIfOptionalLocationNotFound() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        assertDoesNotThrow(() -> {
            runApplication(application, "--spring.config.import=optional:jeap-app-config-aws:failure/config");
        });
    }

    @Test
    void resolvesPropertyFromAppConfig_throwsExceptionIfInvalidLocationArgument() {
        SpringApplication application = new SpringApplication(Application.class);
        application.addBootstrapRegistryInitializer(new AwsConfigurerClientConfigurationMock());

        assertThrows(ConfigDataInvalidLocationArgumentException.class, () ->
                runApplication(application, "--spring.config.import=jeap-app-config-aws:foo_bar"));
    }

    static class AwsConfigurerClientConfigurationMock implements BootstrapRegistryInitializer {

        @Getter
        private int commonPlatformPollCount = 0;
        @Getter
        private int commonPollCount = 0;
        @Getter
        private int appPollCount = 0;
        @Getter
        private int numMissedPollIntervalsForApp = 0;
        @Getter
        private int numUnexpectedSessionTokensReceived = 0;
        private int commonNextPollIntervalSeconds = 5;
        private List<Integer> appNextPollIntervalSecondsList = List.of(5);
        private ZonedDateTime expectedNextAppPollAt = null;
        @Setter
        private boolean commonPlatformFailGetLatestConfigurationRequests;
        @Setter
        private boolean commonFailGetLatestConfigurationRequests;
        @Getter
        private int commonPlatformNumFailedGetLatestConfigurationRequests = 0;
        @Getter
        private int commonNumFailedGetLatestConfigurationRequests = 0;
        private String commonValue = ATTRIBUTE_VALUE_COMMON;
        private boolean commonPlatformChanged = true;
        private boolean commonChanged = true;
        private final Map<String, String> appProperties = new HashMap<>(Map.of(
                ATTRIBUTE_KEY, ATTRIBUTE_VALUE_APP,
                ATTRIBUTE_CONFIG_PROPS_KEY, ATTRIBUTE_CONFIG_PROPS_VALUE,
                ATTRIBUTE_REFRESH_SCOPE_KEY, ATTRIBUTE_REFRESH_SCOPE_VALUE,
                ATTRIBUTE_STANDARD_SCOPE_KEY, ATTRIBUTE_STANDARD_SCOPE_VALUE));
        private boolean appChanged = true;

        AwsConfigurerClientConfigurationMock() {}

        AwsConfigurerClientConfigurationMock(int commonNextPollIntervalSeconds, List<Integer> appNextPollIntervalSecondsList) {
            this();
            this.commonNextPollIntervalSeconds = commonNextPollIntervalSeconds;
            this.appNextPollIntervalSecondsList = appNextPollIntervalSecondsList;
        }

        void changeCommonValue(String newCommonValue) {
            this.commonValue = newCommonValue;
            this.commonChanged = true;
        }

        void changeAppValue(String key, String newValue) {
            appProperties.put(key, newValue);
            this.appChanged = true;
        }

        @Override
        public void initialize(BootstrapRegistry registry) {
            System.setProperty("aws.region", Region.EU_CENTRAL_1.id());
            registry.register(AppConfigDataClient.class,
                    context -> new AppConfigDataClient() {

                        @Override
                        public GetLatestConfigurationResponse getLatestConfiguration(GetLatestConfigurationRequest getLatestConfigurationRequest) {

                            final String sessionToken = getLatestConfigurationRequest.configurationToken();
                            if (sessionToken.startsWith("token-common-platform")) {
                                assertThat(getLatestConfigurationRequest.configurationToken()).isEqualTo("token-common-platform-" + commonPlatformPollCount);
                                if (commonPlatformFailGetLatestConfigurationRequests) {
                                    commonPlatformNumFailedGetLatestConfigurationRequests++;
                                    throw InternalServerException.builder().message("test oops").build();
                                }
                                SdkBytes configuration;
                                if (!commonPlatformChanged) {
                                    configuration = SdkBytes.fromUtf8String("");
                                } else {
                                    configuration = SdkBytes.fromUtf8String("""
                                            {
                                            "jeap.test.platform.common": false
                                            }
                                            """);
                                    commonPlatformChanged = false;
                                }
                                return GetLatestConfigurationResponse.builder()
                                        .configuration(configuration)
                                        .contentType("application/json")
                                        .nextPollConfigurationToken("token-common-platform-" + ++commonPlatformPollCount)
                                        .nextPollIntervalInSeconds(commonNextPollIntervalSeconds)
                                        .build();
                            } else if (sessionToken.startsWith("token-common")) {
                                assertThat(getLatestConfigurationRequest.configurationToken()).isEqualTo("token-common-" + commonPollCount);
                                if (commonFailGetLatestConfigurationRequests) {
                                    commonNumFailedGetLatestConfigurationRequests++;
                                    throw InternalServerException.builder().message("test oops").build();
                                }
                                SdkBytes configuration;
                                if (!commonChanged) {
                                    configuration = SdkBytes.fromUtf8String("");
                                } else {
                                    configuration = SdkBytes.fromUtf8String("""
                                            {"key": "%s",
                                            "jeap.test.common": false
                                            }
                                            """.formatted(commonValue));
                                    commonChanged = false;
                                }
                                return GetLatestConfigurationResponse.builder()
                                        .configuration(configuration)
                                        .contentType("application/json")
                                        .nextPollConfigurationToken("token-common-" + ++commonPollCount)
                                        .nextPollIntervalInSeconds(commonNextPollIntervalSeconds)
                                        .build();
                            } else if (sessionToken.startsWith("token-app")) {
                                assertThat(getLatestConfigurationRequest.configurationToken()).isEqualTo("token-app-" + appPollCount);
                                appPollCount++;
                                final ZonedDateTime currentAppPollAt = ZonedDateTime.now();
                                if ((expectedNextAppPollAt != null) &&
                                    Math.abs(ChronoUnit.MILLIS.between(expectedNextAppPollAt, currentAppPollAt)) > 500){
                                    numMissedPollIntervalsForApp++;
                                }
                                int nextPollIntervalSeconds = appPollCount <= appNextPollIntervalSecondsList.size() ?
                                        appNextPollIntervalSecondsList.get(appPollCount-1) :
                                        appNextPollIntervalSecondsList.get(appNextPollIntervalSecondsList.size()-1);
                                expectedNextAppPollAt = currentAppPollAt.plusSeconds(nextPollIntervalSeconds);
                                SdkBytes configuration ;
                                if (!appChanged) {
                                    configuration = SdkBytes.fromUtf8String("");
                                } else {
                                    configuration = SdkBytes.fromUtf8String("""
                                                {"%s": "%s",
                                                "jeap.test.application": true,
                                                "%s": "%s",
                                                "%s": "%s",
                                                "%s": "%s"
                                                }
                                                """.formatted(
                                                        ATTRIBUTE_KEY, appProperties.get(ATTRIBUTE_KEY),
                                                        ATTRIBUTE_CONFIG_PROPS_KEY, appProperties.get(ATTRIBUTE_CONFIG_PROPS_KEY),
                                                        ATTRIBUTE_REFRESH_SCOPE_KEY, appProperties.get(ATTRIBUTE_REFRESH_SCOPE_KEY),
                                                        ATTRIBUTE_STANDARD_SCOPE_KEY, appProperties.get(ATTRIBUTE_STANDARD_SCOPE_KEY)));
                                    appChanged = false;
                                }
                                return GetLatestConfigurationResponse.builder()
                                        .configuration(configuration)
                                        .contentType("application/json")
                                        .nextPollConfigurationToken("token-app-" + appPollCount)
                                        .nextPollIntervalInSeconds(nextPollIntervalSeconds)
                                        .build();
                            }
                            else {
                                numUnexpectedSessionTokensReceived++;
                                return null;
                            }
                        }

                        @Override
                        public GetLatestConfigurationResponse getLatestConfiguration(Consumer<GetLatestConfigurationRequest.Builder> getLatestConfigurationRequest) throws AwsServiceException, SdkClientException {
                            return AppConfigDataClient.super.getLatestConfiguration(getLatestConfigurationRequest);
                        }

                        @Override
                        public StartConfigurationSessionResponse startConfigurationSession(StartConfigurationSessionRequest startConfigurationSessionRequest) throws AwsServiceException, SdkClientException {
                            assertThat(startConfigurationSessionRequest.environmentIdentifier()).isEqualTo("dev");

                            if (startConfigurationSessionRequest.applicationIdentifier().equalsIgnoreCase("failure")) {
                                throw new RuntimeException("Mock failure to app config");
                            }

                            final String appId = startConfigurationSessionRequest.applicationIdentifier();
                            return StartConfigurationSessionResponse.builder()
                                    .initialConfigurationToken("token-" + appId + "-" +
                                            switch(appId) {
                                                case "common" -> commonPollCount;
                                                case "app" -> appPollCount;
                                                default -> 0;})
                                    .build();
                        }

                        @Override
                        public StartConfigurationSessionResponse startConfigurationSession(Consumer<StartConfigurationSessionRequest.Builder> startConfigurationSessionRequest) throws AwsServiceException, SdkClientException {
                            return AppConfigDataClient.super.startConfigurationSession(startConfigurationSessionRequest);
                        }

                        @Override
                        public AppConfigDataServiceClientConfiguration serviceClientConfiguration() {
                            return AppConfigDataClient.super.serviceClientConfiguration();
                        }

                        @Override
                        public void close() {

                        }

                        @Override
                        public String serviceName() {
                            return null;
                        }

                    });
        }
    }

}
