package ch.admin.bit.jeap.swagger;

import ch.admin.bit.jeap.monitor.ActuatorSecurity;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for the Actuator-API. Load this only if actuator monitoring starter is loaded
 */
@AutoConfiguration
@ConditionalOnProperty(value = "springdoc.show-actuator", matchIfMissing = true)
@ConditionalOnClass(ActuatorSecurity.class)
@SecurityScheme(
        name = "prometheus",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
@Slf4j
class ActuatorSwaggerConfig {

    @Bean
    GroupedOpenApi actuatorApi(@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
        return GroupedOpenApi.builder()
                .group("Actuator")
                .pathsToMatch(getActuatorPathsToMatch(actuatorBasePath))
                .addOpenApiCustomizer(this::changeInfo)
                .build();
    }

    private void changeInfo(OpenAPI openAPI) {
        openAPI.getInfo().setContact(null);
        openAPI.getInfo().setTitle("Monitoring Endpunkte");
        openAPI.getInfo().setDescription("Monitoring Endpunkte des Spring Actuator");
        if (openAPI.getExternalDocs() != null) {
            openAPI.getExternalDocs().setDescription("Monitoring im Blueprint Microservice");
            openAPI.getExternalDocs().setUrl("https://confluence.bit.admin.ch/display/JEAP/Monitoring");
        }
        openAPI.addSecurityItem(new SecurityRequirement().addList("prometheus"));
    }

    private String[] getActuatorPathsToMatch(String actuatorBasePath) {
        log.debug("Configured actuator base path: {}", actuatorBasePath);
        // Strip trailing slash from actuator base path if present
        actuatorBasePath = actuatorBasePath.replaceAll("/$", "");
        // Note: actuator base path always starts with a slash
        String matchAllActuatorPathsPattern =  actuatorBasePath + "/**";
        log.debug("Matching actuator paths for Swagger Actuator API group: {}", matchAllActuatorPathsPattern);
        return new String[]{matchAllActuatorPathsPattern};
    }

}
