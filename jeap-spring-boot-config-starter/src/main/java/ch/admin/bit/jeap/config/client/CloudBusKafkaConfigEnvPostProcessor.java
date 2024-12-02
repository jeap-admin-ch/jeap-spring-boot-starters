package ch.admin.bit.jeap.config.client;

import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class CloudBusKafkaConfigEnvPostProcessor  implements EnvironmentPostProcessor {

    private static final String SASL_MECHANISM = "SCRAM-SHA-512";
    private static final String SASL_JAAS_TEMPLATE = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";

    private static final String CLOUD_BUS_PROPERTY_SOURCE_NAME = "cloudBusProperties";
    private static final String CLOUD_BUS_KAFKA_BROKERS_KEY = "spring.cloud.stream.kafka.binder.brokers";

    private final Log log;

    /**
     * Configure cloud bus kafka properties based on jeap cloud bus properties or jeap messaging properties.
     * If called more than once will each time replace a configuration previously added by this environment post processor
     * with a new configuration based on the properties currently known to the environment.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        log.debug("CloudBusKafkaConfigEnvPostProcessor starts configuring cloud bus kafka connection.");
        Map<String, Object> cloudBusKafkaProps = new HashMap<>();
        configureCommonCloudBusKafkaProperties(cloudBusKafkaProps);

        JeapCloudBusKafkaProperties jeapCloudBusKafkaProps = JeapCloudBusKafkaProperties.from(environment);
        JeapMessagingKafkaProperties jeapMessagingKafkaProps = JeapMessagingKafkaProperties.from(environment);
        if (jeapCloudBusKafkaProps.isConfigured()) {
            log.debug("Found a jEAP cloud bus config -> configuring cloud bus based on those settings.");
            configureCloudBusKafkaProperties(cloudBusKafkaProps, jeapCloudBusKafkaProps, environment);
        } else if (jeapMessagingKafkaProps.isConfigured()) {
            log.debug("No jEAP cloud bus config found, but there is a jEAP messaging config -> configuring cloud bus based on the jEAP messaging settings.");
            configureCloudBusKafkaProperties(cloudBusKafkaProps, jeapMessagingKafkaProps, environment);
        } else {
            log.debug("No jEAP cloud bus config and no jEAP messaging config found -> won't configure cloud bus.");
            cloudBusKafkaProps.clear();
        }

        // Add configuration with the lowest priority in order to allow overriding
        // (will replace an existing cloud bus property source if one has already been registered before)
        environment.getPropertySources().addLast(createCloudBusPropertySource(cloudBusKafkaProps));
        log.debug("CloudBusKafkaConfigEnvPostProcessor finished configuring cloud bus kafka connection.");
    }

    private void configureCommonCloudBusKafkaProperties(Map<String, Object> targetProps) {
        targetProps.put("spring.cloud.stream.kafka.binder.auto-create-topics", "false");
    }

    private PropertySource<?> createCloudBusPropertySource(Map<String, Object> props) {
        return new MapPropertySource(CLOUD_BUS_PROPERTY_SOURCE_NAME, props);
    }

    private void configureCloudBusKafkaProperties(Map<String, Object> targetProps, JeapCloudBusKafkaProperties jeapCloudBusKafkaProperties, Environment environment) {
        String jeapCloudBusBrokers = jeapCloudBusKafkaProperties.getBootstrapServers();
        log.info("Configuring cloud bus kafka connection with brokers given by the jEAP cloud bus config (" + jeapCloudBusBrokers + ").");
        targetProps.put(CLOUD_BUS_KAFKA_BROKERS_KEY, jeapCloudBusBrokers);
        addSecurityConfig(targetProps, jeapCloudBusKafkaProperties, environment);
    }

    private void configureCloudBusKafkaProperties(Map<String, Object> targetProps, JeapMessagingKafkaProperties jeapMessagingKafkaProperties, Environment environment) {
        if (jeapMessagingKafkaProperties.getConsumerBootstrapServers() != null) {
            String jeapMessagingConsumerBrokers = jeapMessagingKafkaProperties.getConsumerBootstrapServers();
            log.info("Configuring cloud bus kafka connection with consumer brokers from jEAP messaging (" + jeapMessagingConsumerBrokers + ").");
            targetProps.put(CLOUD_BUS_KAFKA_BROKERS_KEY, jeapMessagingConsumerBrokers);
        } else {
            String jeapMessagingBrokers = jeapMessagingKafkaProperties.getBootstrapServers();
            log.info("Configuring cloud bus kafka connection with brokers from jEAP messaging (" + jeapMessagingBrokers + ").");
            targetProps.put(CLOUD_BUS_KAFKA_BROKERS_KEY, jeapMessagingBrokers);
        }
        addSecurityConfig(targetProps, jeapMessagingKafkaProperties, environment );
    }

    private void addSecurityConfig(Map<String, Object> targetProps, JeapKafkaProperties config, Environment environment) {
        targetProps.put("spring.cloud.stream.kafka.binder.configuration.security.protocol", config.getSecurityProtocol());
        if (config.isSasl()) {
            log.debug("Configuring cloud bus kafka connection with SASL (" + SASL_MECHANISM + ").");
            targetProps.put("spring.cloud.stream.kafka.binder.configuration.sasl.mechanism", SASL_MECHANISM);
            String saslJaasConfig = String.format(SASL_JAAS_TEMPLATE, config.getUsername(), config.getPassword());
            targetProps.put("spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config", saslJaasConfig);
        }
        if (config.isSsl()) {
            String truststoreLocation = environment.getProperty("javax.net.ssl.trustStore");
            log.debug("Configuring cloud bus kafka connection with SSL (truststore: " + truststoreLocation + ").");
            targetProps.put("spring.cloud.stream.kafka.binder.configuration.ssl.truststore.location", truststoreLocation);
            targetProps.put("spring.cloud.stream.kafka.binder.configuration.ssl.truststore.password",
                    environment.getProperty("javax.net.ssl.trustStorePassword"));
        }
    }

}
