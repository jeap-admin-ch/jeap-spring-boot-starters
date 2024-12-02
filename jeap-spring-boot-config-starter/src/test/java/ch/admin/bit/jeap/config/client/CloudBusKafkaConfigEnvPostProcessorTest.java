package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CloudBusKafkaConfigEnvPostProcessorTest {

    private static final Log LOG = LogFactory.getLog(CloudBusKafkaConfigEnvPostProcessorTest.class);

    private static final String PROTOCOL_PLAINTEXT = "PLAINTEXT";
    private static final String PROTOCOL_SASL_SSL = "SASL_SSL";
    private static final String SASL_MECHANISM = "SCRAM-SHA-512";
    private static final String JAVAX_TRUSTSTORE_LOCATION_KEY = "javax.net.ssl.trustStore";
    private static final String JAVAX_TRUSTSTORE_PASSWORD_KEY = "javax.net.ssl.trustStorePassword";

    // jEAP cloud bus configuration properties
    private static final String JCB_BROKERS_KEY = "jeap.config.client.cloud.bus.kafka.bootstrap-servers";
    private static final String JCB_PROTOCOL_KEY = "jeap.config.client.cloud.bus.kafka.security-protocol";
    private static final String JCB_USERNAME_KEY = "jeap.config.client.cloud.bus.kafka.username";
    private static final String JCB_PASSWORD_KEY = "jeap.config.client.cloud.bus.kafka.password";

    // jEAP messaging configuration properties
    private static final String JM_BROKERS_KEY = "jeap.messaging.kafka.bootstrap-servers";
    private static final String JM_CONSUMER_BROKERS_KEY = "jeap.messaging.kafka.consumer-bootstrap-servers";
    private static final String JM_PRODUCER_BROKERS_KEY = "jeap.messaging.kafka.producer-bootstrap-servers";
    private static final String JM_ADMIN_CLIENT_BROKERS_KEY = "jeap.messaging.kafka.admin-client-bootstrap-servers";
    private static final String JM_PROTOCOL_KEY = "jeap.messaging.kafka.security-protocol";
    private static final String JM_USERNAME_KEY = "jeap.messaging.kafka.username";
    private static final String JM_PASSWORD_KEY = "jeap.messaging.kafka.password";

    // Cloud bus configuration properties
    private static final String CB_BROKERS_KEY = "spring.cloud.stream.kafka.binder.brokers";
    private static final String CB_PROTOCOL_KEY = "spring.cloud.stream.kafka.binder.configuration.security.protocol";
    private static final String CB_SASL_MECHANISM_KEY = "spring.cloud.stream.kafka.binder.configuration.sasl.mechanism";
    private static final String CB_SASL_JAAS_KEY = "spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config";
    private static final String CB_TRUSTSTORE_LOCATION_KEY = "spring.cloud.stream.kafka.binder.configuration.ssl.truststore.location";
    private static final String CB_TRUSTSTORE_PASSWORD_KEY = "spring.cloud.stream.kafka.binder.configuration.ssl.truststore.password";

    private MockEnvironment mockEnvironment;

    @BeforeEach
    void initialize() {
        mockEnvironment = new MockEnvironment();
    }


    @Test
    void testBrokers_whenOnlyJeapCloudBusConfigGiven_thenFromJeapCloudBusConfig() {
        mockEnvironment.withProperty(JCB_BROKERS_KEY, "jeap cloud bus");
        mockEnvironment.withProperty(JCB_PROTOCOL_KEY, PROTOCOL_PLAINTEXT);
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap cloud bus");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_PLAINTEXT);
    }

    @Test
    void testBrokers_whenOnlyJeapMessagingConfigGiven_thenFromJeapMessagingConfig() {
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, PROTOCOL_PLAINTEXT);
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap messaging");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_PLAINTEXT);
    }

    @Test
    void testBrokers_whenOnlySplitJeapMessagingConfigGiven_thenFromJeapMessagingComsumerConfig() {
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_CONSUMER_BROKERS_KEY, "jeap messaging consumers");
        mockEnvironment.withProperty(JM_PRODUCER_BROKERS_KEY, "jeap messaging producers");
        mockEnvironment.withProperty(JM_ADMIN_CLIENT_BROKERS_KEY, "jeap messaging admin clients");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, PROTOCOL_PLAINTEXT);
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap messaging consumers");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_PLAINTEXT);
    }

    @Test
    void testBrokers_whenJeapCloudBusConfigAndJeapMessagingConfigGiven_thenFromJeapCloudBusConfig() {
        mockEnvironment.withProperty(JCB_BROKERS_KEY, "jeap cloud bus");
        mockEnvironment.withProperty(JCB_PROTOCOL_KEY, PROTOCOL_PLAINTEXT);
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, "jeap messaging");
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap cloud bus");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_PLAINTEXT);
    }

    @Test
    void testBrokers_whenJeapCloudBusConfigAndSplitJeapMessagingConfigGiven_thenFromJeapCloudBusConfig() {
        mockEnvironment.withProperty(JCB_BROKERS_KEY, "jeap cloud bus");
        mockEnvironment.withProperty(JCB_PROTOCOL_KEY, PROTOCOL_PLAINTEXT);
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_CONSUMER_BROKERS_KEY, "jeap messaging consumers");
        mockEnvironment.withProperty(JM_PRODUCER_BROKERS_KEY, "jeap messaging producers");
        mockEnvironment.withProperty(JM_ADMIN_CLIENT_BROKERS_KEY, "jeap messaging admin clients");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, "jeap messaging");
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap cloud bus");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_PLAINTEXT);
    }

    @Test
    void testSaslSssl_whenOnlyJeapCloudBusConfigGiven_thenFromJeapCloudBusConfig() {
        mockEnvironment.withProperty(JCB_BROKERS_KEY, "jeap cloud bus");
        mockEnvironment.withProperty(JCB_PROTOCOL_KEY, PROTOCOL_SASL_SSL);
        mockEnvironment.withProperty(JCB_USERNAME_KEY, "jeap cloud bus user");
        mockEnvironment.withProperty(JCB_PASSWORD_KEY, "jeap cloud bus password");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_LOCATION_KEY, "javax truststore location");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_PASSWORD_KEY, "javax truststore password");
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap cloud bus");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_SASL_SSL);
        assertThat(mockEnvironment.getProperty(CB_SASL_MECHANISM_KEY)).isEqualTo(SASL_MECHANISM);
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap cloud bus user");
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap cloud bus password");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_LOCATION_KEY)).isEqualTo("javax truststore location");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_PASSWORD_KEY)).isEqualTo("javax truststore password");
    }

    @Test
    void testSaslSssl_whenOnlyJeapMessagingConfigGiven_thenFromJeapMessagingConfig() {
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, PROTOCOL_SASL_SSL);
        mockEnvironment.withProperty(JM_USERNAME_KEY, "jeap messaging user");
        mockEnvironment.withProperty(JM_PASSWORD_KEY, "jeap messaging password");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_LOCATION_KEY, "javax truststore location");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_PASSWORD_KEY, "javax truststore password");
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap messaging");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_SASL_SSL);
        assertThat(mockEnvironment.getProperty(CB_SASL_MECHANISM_KEY)).isEqualTo(SASL_MECHANISM);
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap messaging user");
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap messaging password");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_LOCATION_KEY)).isEqualTo("javax truststore location");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_PASSWORD_KEY)).isEqualTo("javax truststore password");
    }

    @Test
    void testSaslSssl_whenJeapCloudBusConfigAndJeapMessagingConfigGiven_thenFromJeapCloudBusConfig() {
        mockEnvironment.withProperty(JCB_BROKERS_KEY, "jeap cloud bus");
        mockEnvironment.withProperty(JCB_PROTOCOL_KEY, PROTOCOL_SASL_SSL);
        mockEnvironment.withProperty(JCB_USERNAME_KEY, "jeap cloud bus user");
        mockEnvironment.withProperty(JCB_PASSWORD_KEY, "jeap cloud bus password");
        mockEnvironment.withProperty(JM_BROKERS_KEY, "jeap messaging");
        mockEnvironment.withProperty(JM_PROTOCOL_KEY, "SASL_jeap_messaging_SSL");
        mockEnvironment.withProperty(JM_USERNAME_KEY, "jeap messaging user");
        mockEnvironment.withProperty(JM_PASSWORD_KEY, "jeap messaging password");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_LOCATION_KEY, "javax truststore location");
        mockEnvironment.withProperty(JAVAX_TRUSTSTORE_PASSWORD_KEY, "javax truststore password");
        CloudBusKafkaConfigEnvPostProcessor envPostProcessor = new  CloudBusKafkaConfigEnvPostProcessor(LOG);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(CB_BROKERS_KEY)).isEqualTo("jeap cloud bus");
        assertThat(mockEnvironment.getProperty(CB_PROTOCOL_KEY)).isEqualTo(PROTOCOL_SASL_SSL);
        assertThat(mockEnvironment.getProperty(CB_SASL_MECHANISM_KEY)).isEqualTo(SASL_MECHANISM);
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap cloud bus user");
        assertThat(mockEnvironment.getProperty(CB_SASL_JAAS_KEY)).contains("jeap cloud bus password");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_LOCATION_KEY)).isEqualTo("javax truststore location");
        assertThat(mockEnvironment.getProperty(CB_TRUSTSTORE_PASSWORD_KEY)).isEqualTo("javax truststore password");
    }

}
