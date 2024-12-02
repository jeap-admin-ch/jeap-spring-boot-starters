package ch.admin.bit.jeap.config.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two Tests are checking, if the Application does NOT start. That's why the 'ConfigClientTestInstance'
 * will be started with the main()-Method
 */
@Slf4j
class FailFastIT {

    @Test
    @DirtiesContext
    void test_WhenConfigServerNotAccessibleInBootstrapContext_thenFailFastWithExceptionDuringStartup() {
        try {
            System.setProperty("spring.cloud.bootstrap.enabled", "true");
            // No properties like e.g. config server url defined
            Map<String, Object> propertiesMap = Map.of();

            assertThatThrownBy( () -> ConfigClientTestInstance.main(propertiesMap)).
                    hasMessage("Could not locate PropertySource and the fail fast property is set, failing");
        }
        finally {
            System.clearProperty("spring.cloud.bootstrap.enabled");
        }
    }

    @Test
    @DirtiesContext
    void test_WhenConfigServerNotAccessibleForConfigImportMechanism_thenFailFastWithExceptionDuringStartup() {
        assertThat(System.getProperty("spring.cloud.bootstrap.enabled", "false")).isEqualToIgnoringCase("false");
        // activate config server import but do not define needed properties like e.g. the config server url
        Map<String, Object> propertiesMap = Map.of("spring.config.import", "configserver:");

        assertThatThrownBy( () -> ConfigClientTestInstance.main(propertiesMap)).
                hasMessage("Could not locate PropertySource and the fail fast property is set, failing");
    }

    @Test
    @DirtiesContext
    void test_WhenConfigServerNotAccessibleForConfigImportMechanismAndFailFastFalseAndConfigOptional_thenStartupOk() {
        assertThat(System.getProperty("spring.cloud.bootstrap.enabled", "false")).isEqualToIgnoringCase("false");
        // activate config server import optional
        Map<String, Object> propertiesMap = Map.of("spring.config.import", "optional:configserver:",
                "jeap.config.client.fail-fast", "false",
                "spring.cloud.bus.destination", "someTopicName",
                "spring.application.name", "someApplicationName",
                "spring.cloud.bus.enabled", "false");

        ConfigClientTestInstance.main(propertiesMap);
    }

    @Test
    @DirtiesContext
    void test_WhenConfigServerNotAccessibleForConfigImportMechanismAndFailFastFalseAndConfigNotOptional_thenFailFastWithExceptionDuringStartup() {
        assertThat(System.getProperty("spring.cloud.bootstrap.enabled", "false")).isEqualToIgnoringCase("false");
        // activate config server import optional
        Map<String, Object> propertiesMap = Map.of("spring.config.import", "configserver:",
                "jeap.config.client.fail-fast", "false",
                "spring.cloud.bus.destination", "someTopicName",
                "spring.application.name", "someApplicationName",
                "spring.cloud.bus.enabled", "false");

        assertThatThrownBy( () -> ConfigClientTestInstance.main(propertiesMap)).
                hasMessage("Could not locate PropertySource and the resource is not optional, failing");
    }


    /**
     * This Test expects, that the ConfigurationGuard can not be created because
     * of the missing Topic Name for the SpringCloudBus
     */
    @Test
    @DirtiesContext
    void expectConfigGuardExceptionDuringStartup() {
        // Set fail-fast to false, otherwise the ConfigGuard will not be affected.
        Map<String, Object> propertiesMap = Map.of("spring.cloud.config.fail-fast", "false");

        Assertions.assertThrows(Exception.class, () ->
                ConfigClientTestInstance.main(propertiesMap));
    }

    /**
     * This is only the proof, that the Application can start with the minimal set of properties
     */
    @Test
    @DirtiesContext
    void testStartUpWithMandatoryConfig() {
        // Set fail-fast to false, otherwise the ConfigGuard will not be affected.
        Map<String, Object> propertiesMap = Map.of(
                "spring.cloud.config.fail-fast", "false",
                "spring.cloud.bus.destination", "someTopicName",
                "spring.application.name", "someApplicationName",
                "spring.cloud.config.enabled", "false",
                "spring.cloud.bus.enabled", "false"
                );

        ConfigClientTestInstance.main(propertiesMap);
        Assertions.assertTrue(true);
    }

}
