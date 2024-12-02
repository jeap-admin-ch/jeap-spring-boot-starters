package ch.admin.bit.jeap.config.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static ch.admin.bit.jeap.config.client.ClientDefaultConfigEnvPostProcessor.CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME;
import static ch.admin.bit.jeap.config.client.ConfigServerAuthFromVaultConfig.AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class PrioritizeConfigServerAuthFromVaultEnvPostProcessorTest {

    private static final Log LOG = LogFactory.getLog(CloudBusKafkaConfigEnvPostProcessorTest.class);
    private static final String KEY = "key";
    private static final String VALUE_DEFAULT = "default";
    private static final String VALUE_VAULT = "vault";

    private MockEnvironment mockEnvironment;

    @BeforeEach
    void initialize() {
        mockEnvironment = new MockEnvironment();
    }

    @Test
    void testPostProcess_whenDefaultBeforeAuth_thenReprioritize() {
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME, KEY, VALUE_DEFAULT));
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME, KEY, VALUE_VAULT));
        PrioritizeConfigServerAuthFromVaultEnvPostProcessor envPostProcessor = new  PrioritizeConfigServerAuthFromVaultEnvPostProcessor(LOG);
        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_DEFAULT);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_VAULT);
    }

    @Test
    void testPostProcess_whenAuthBeforeDefault_thenUnchanged() {
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME, KEY, VALUE_VAULT));
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME, KEY, VALUE_DEFAULT));
        PrioritizeConfigServerAuthFromVaultEnvPostProcessor envPostProcessor = new  PrioritizeConfigServerAuthFromVaultEnvPostProcessor(LOG);
        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_VAULT);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_VAULT);
    }

    @Test
    void testPostProcess_whenAuthMissing_thenUnchanged() {
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(AUTH_FROM_VAULT_PROPERTY_SOURCE_NAME, KEY, VALUE_VAULT));
        PrioritizeConfigServerAuthFromVaultEnvPostProcessor envPostProcessor = new  PrioritizeConfigServerAuthFromVaultEnvPostProcessor(LOG);
        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_VAULT);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_VAULT);
    }

    @Test
    void testPostProcess_whenDefaultMissing_thenUnchanged() {
        mockEnvironment.getPropertySources().addLast(
                getPropertySource(CLIENT_DEFAULT_CONFIG_PROPERTY_SOURCE_NAME, KEY, VALUE_DEFAULT));
        PrioritizeConfigServerAuthFromVaultEnvPostProcessor envPostProcessor = new  PrioritizeConfigServerAuthFromVaultEnvPostProcessor(LOG);
        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_DEFAULT);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(KEY)).isEqualTo(VALUE_DEFAULT);
    }

    @Test
    void testPostProcess_whenAuthAndDefaultMissing_thenUnchanged() {
        final String otherKey = "other-key";
        final String otherValue = "other-value";
        mockEnvironment.getPropertySources().addLast(
                getPropertySource("some-other-source", otherKey, otherValue));
        PrioritizeConfigServerAuthFromVaultEnvPostProcessor envPostProcessor = new  PrioritizeConfigServerAuthFromVaultEnvPostProcessor(LOG);
        assertThat(mockEnvironment.getProperty(otherKey)).isEqualTo(otherValue);

        envPostProcessor.postProcessEnvironment(mockEnvironment, null);

        assertThat(mockEnvironment.getProperty(otherKey)).isEqualTo(otherValue);
    }

    private static PropertySource<?> getPropertySource(String name, String key, String value) {
        return new MapPropertySource(name, Map.of(key, value));
    }

}
