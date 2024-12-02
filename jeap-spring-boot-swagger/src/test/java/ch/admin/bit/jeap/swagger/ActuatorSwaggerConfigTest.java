package ch.admin.bit.jeap.swagger;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorSwaggerConfigTest {

    private final ActuatorSwaggerConfig actuatorSwaggerConfig = new ActuatorSwaggerConfig();

    @Test
    void testActuatorApi_whenBasePathWithoutTrailingSlashConfigured_thenMatchAllPathsUnderBasePath() {
        GroupedOpenApi groupedOpenApi = actuatorSwaggerConfig.actuatorApi("/management");
        assertThat(groupedOpenApi.getPathsToMatch()).containsOnly("/management/**");
    }

    @Test
    void testActuatorApi_whenBasePathWithTrailingSlashConfigured_thenMatchAllPathsUnderBasePathWithoutDoublingSlash() {
        GroupedOpenApi groupedOpenApi = actuatorSwaggerConfig.actuatorApi("/management/");
        assertThat(groupedOpenApi.getPathsToMatch()).containsOnly("/management/**");
    }

    @Test
    void testActuatorApi_whenEmptyBasePathConfigured_thenMatchAllPathsUnderRoot() {
        GroupedOpenApi groupedOpenApi = actuatorSwaggerConfig.actuatorApi("");
        assertThat(groupedOpenApi.getPathsToMatch()).containsOnly("/**");
    }

}
