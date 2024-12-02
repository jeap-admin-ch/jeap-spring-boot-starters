package ch.admin.bit.jeap.config.aws.appconfig.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class JeapAppConfigDataClient {

    private static final int NEXT_POLL_INTERVAL_SECONDS_FALLBACK = 60;

    /**
     * Logger will be post-processed and cannot be final
     */
    @SuppressWarnings("FieldMayBeFinal")
    private static Log log = LogFactory.getLog(JeapAppConfigDataClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final AppConfigDataClient appConfigDataClient;
    private final String appId;
    private final String envId;
    private final String profileId;
    private Integer requiredMinimumPollIntervalInSeconds;
    private ThreadPoolTaskScheduler taskScheduler;
    private String sessionToken;
    private Properties properties;
    private Integer currentPollIntervalSeconds;
    private ZonedDateTime nextPollDateTime = ZonedDateTime.now();
    private JeapAppConfigDataChangedListener changedListener;


    // Non-polling jeap app config client
    JeapAppConfigDataClient(AppConfigDataClient appConfigDataClient, String appId, String envId, String profileId) {
        this.appConfigDataClient = appConfigDataClient;
        this.appId = appId;
        this.envId = envId;
        this.profileId = profileId;
    }

    // Polling jeap app config client
    JeapAppConfigDataClient(AppConfigDataClient appConfigDataClient, String appId, String envId, String profileId, Integer requiredMinimumPollIntervalInSeconds) {
        this(appConfigDataClient, appId, envId, profileId);
        this.requiredMinimumPollIntervalInSeconds = requiredMinimumPollIntervalInSeconds;
        this.currentPollIntervalSeconds = Optional.ofNullable(requiredMinimumPollIntervalInSeconds).orElse(NEXT_POLL_INTERVAL_SECONDS_FALLBACK);
        this.taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("jeap-aws-appconfig-polling-scheduler-");
        taskScheduler.setDaemon(true);
        taskScheduler.afterPropertiesSet();
    }

    public Properties getProperties() {
        if (properties == null) {
            retrieveCurrentConfiguration();
        }
        return properties;
    }

    public void setConfigDataChangedListener(JeapAppConfigDataChangedListener listener) {
        this.changedListener = listener;
    }

    @SuppressWarnings("java:S106")
    void retrieveCurrentConfiguration() {
        if (!ZonedDateTime.now().isBefore(nextPollDateTime)) {
            log.trace("Retrieving current config " + appConfigInfo());
            if (sessionToken == null) {
                initializeSession();
            }

            GetLatestConfigurationResponse latestConfigurationResponse = null;
            try {
                latestConfigurationResponse = getLatestConfiguration();
            } catch (Exception e) {
                if (properties == null) {
                    // If the properties have not yet been successfully fetched, this was the first call to the appconfig service during the application startup. 
                    // At this time, the logging system has not yet been initialized. Therefore, we have to write the error directly to the console.
                    e.printStackTrace(System.err);
                }
                log.error("Getting latest configuration from AppConfig service failed for " + appConfigInfo(), e);
                // try again with the next poll...
                return;
            }
            finally {
                scheduleNextPoll(latestConfigurationResponse);
            }

            if (loadPropertiesIfChanged(latestConfigurationResponse)) {
                Optional.ofNullable(changedListener).ifPresent(listener -> listener.appConfigDataChanged(profileId));
            }
        } else {
            log.debug("Request to retrieve the current config of " + appConfigInfo() +
                      " happened before the next scheduled poll at " + nextPollDateTimeString() + " -> request ignored.");
        }
    }

    synchronized void disablePolling() {
        taskScheduler = null;
    }

    /**
     * Load the configuration properties from the config data given by the latest configuration response. If the response
     * does not contain data, keep the current configuration properties.
     * @param latestConfigurationResponse The latest configuration response from AWS AppConfig
     * @return <code>true</code> if existing current configuration properties were replaced with new config data.
     */
    private boolean loadPropertiesIfChanged(GetLatestConfigurationResponse latestConfigurationResponse) {
        final SdkBytes configuration = latestConfigurationResponse.configuration();
        if (configuration.asByteArray().length == 0) {
            log.trace("Latest configuration of app config " + appConfigInfo() + " unchanged, no properties reloaded.");
            return false;
        }
        boolean isInitialLoad = (properties == null);
        final String contentType = latestConfigurationResponse.contentType();
        log.debug("Loading properties from latest configuration of app config " + appConfigInfo() + " with content type " + contentType);
        if (contentType.equals("application/x-yaml")) {
            properties = retrievePropertiesFromYaml(configuration.asByteArray());
        } else if (contentType.equals("application/json")) {
            properties = retrievePropertiesFromJson(configuration.asByteArray());
        } else {
            properties = retrievePropertiesFromText(configuration.asUtf8String());
        }
        log.trace("Loaded properties from latest configuration of app config " + appConfigInfo() + ": " + this.properties);
        return !isInitialLoad;
    }

    private static Properties retrievePropertiesFromYaml(byte[] content) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ByteArrayResource(content));
        return factory.getObject();
    }

    private static Properties retrievePropertiesFromText(String content) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(content));
            return props;
        } catch (IOException e) {
            throw new AppConfigParsingException("Error during parsing of properties content", e);
        }
    }

    static class AppConfigParsingException extends RuntimeException {

        public AppConfigParsingException(String message, IOException cause) {
            super(message, cause);
        }

    }

    private Properties retrievePropertiesFromJson(byte[] content) {
        try {
            Map<String, Object> map = mapper.readValue(content, new TypeReference<>() {
            });
            Properties props = new Properties();
            props.putAll(map);
            return props;
        } catch (IOException e) {
            throw new AppConfigParsingException("Error during parsing of json content", e);
        }
    }

    private void initializeSession() {
        log.info("Initializing session for " + appConfigInfo());
        StartConfigurationSessionRequest.Builder startConfigurationSessionRequestBuilder = StartConfigurationSessionRequest.builder()
                .applicationIdentifier(appId)
                .environmentIdentifier(envId)
                .configurationProfileIdentifier(profileId);
        Optional.ofNullable(requiredMinimumPollIntervalInSeconds)
                .ifPresent(startConfigurationSessionRequestBuilder::requiredMinimumPollIntervalInSeconds);
        sessionToken = appConfigDataClient.startConfigurationSession(startConfigurationSessionRequestBuilder.build())
                                          .initialConfigurationToken();
        log.trace("Initialized session for " + appConfigInfo());
    }

    private GetLatestConfigurationResponse getLatestConfiguration() {
        log.trace("Getting latest configuration " + appConfigInfo());
        var latestConfigurationResponse = appConfigDataClient.getLatestConfiguration(
                GetLatestConfigurationRequest.builder().configurationToken(sessionToken).build()
        );
        sessionToken = latestConfigurationResponse.nextPollConfigurationToken();
        return latestConfigurationResponse;
    }


    synchronized private void scheduleNextPoll(GetLatestConfigurationResponse latestConfigurationResponse) {
        if (taskScheduler != null) { // polling is disabled if no scheduler is provided
            int nextPollIntervalSeconds = Optional.ofNullable(latestConfigurationResponse)
                    .map(this::getNextPollIntervalSeconds)
                    .orElse(currentPollIntervalSeconds);
            log.trace("Next poll interval is " + nextPollIntervalSeconds + " seconds for app config " + appConfigInfo());
            nextPollDateTime = ZonedDateTime.now().plusSeconds(nextPollIntervalSeconds);
            log.trace("Scheduling next poll at " + nextPollDateTimeString() + " for app config " + appConfigInfo());
            taskScheduler.schedule(this::retrieveCurrentConfiguration, nextPollDateTime.toInstant());
            currentPollIntervalSeconds = nextPollIntervalSeconds;
        }
    }

    private int getNextPollIntervalSeconds(GetLatestConfigurationResponse latestConfigurationResponse) {
        return Optional.ofNullable(latestConfigurationResponse.nextPollIntervalInSeconds())
                    .orElse(Optional.ofNullable(requiredMinimumPollIntervalInSeconds)
                    .orElse(NEXT_POLL_INTERVAL_SECONDS_FALLBACK));
    }

    private String appConfigInfo() {
        return "{" +
                "appId='" + appId + '\'' +
                ", envId='" + envId + '\'' +
                ", profileId='" + profileId + '\'' +
                '}';
    }

    private String nextPollDateTimeString() {
        return nextPollDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

}
