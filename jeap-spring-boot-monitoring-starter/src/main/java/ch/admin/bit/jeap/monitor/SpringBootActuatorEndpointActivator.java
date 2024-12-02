package ch.admin.bit.jeap.monitor;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Activates Spring Actuator endpoints used by Spring Boot Admin if the property jeap.monitor.actuator.expose-admin-endpoints
 * is set to true.
 */
@Slf4j
class SpringBootActuatorEndpointActivator implements EnvironmentPostProcessor {

    private static final String CONFIG_FILE = "jeap-actuator-spring-boot.properties";
    private static final String ENABLE_ENDPOINT_BY_ID_TEMPLATE = "management.endpoint.%s.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty(MonitoringConfig.ENABLE_ADMIN_ENDPOINTS_PROPERTY, Boolean.class, false)) {
            enableSpringBootActuatorProperties(environment);
            enableAdditionalPermittedEndpoints(environment);
            enableLogfileAccessForLogfileEndpoint(environment);
        }
    }

    @SneakyThrows
    private void enableSpringBootActuatorProperties(ConfigurableEnvironment environment) {
        PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
        Resource resource = new ClassPathResource(CONFIG_FILE);
        loader.load(CONFIG_FILE, resource).forEach(environment.getPropertySources()::addLast);
    }

    private void enableAdditionalPermittedEndpoints(ConfigurableEnvironment environment) {
        String additionalPermittedEndpoints = environment.getProperty(MonitoringConfig.ADDITIONAL_PERMITTED_ENDPOINTS_PROPERTY);
        if (StringUtils.isNotBlank(additionalPermittedEndpoints)) {
            Map<String, Object> endpointEnablingProperties = getEndpointEnablingProperties(additionalPermittedEndpoints);
            if (!endpointEnablingProperties.isEmpty()) {
                addPropertiesToEnvironment(endpointEnablingProperties, environment);
            }
        }
    }

    private void enableLogfileAccessForLogfileEndpoint(ConfigurableEnvironment environment) {
        // The logfile actuator needs the 'logging.file.name' property to be set to get activated and the property must reference the logfile.
        // The jeap-spring-boot-logging-starter has a 'logback-spring.xml' configuration that configures the logging file appender for the cloud profile.
        // For the logfile actuator to work, we have to match the 'logback-spring.xml' configuration here with the 'logging.file.name' property.
        if (Set.of(environment.getActiveProfiles()).contains("cloud")) {
            Map<String, Object> logFileProperties = new HashMap<>();
            logFileProperties.put("logging.file.name", "log.log");
            MapPropertySource propertySource = new MapPropertySource("spring-logfile-properties", logFileProperties);
            environment.getPropertySources().addLast(propertySource);
        }
    }

    private Map<String, Object> getEndpointEnablingProperties(String additionalPermittedEndpoints) {
        return  getAdditionalPermittedEndpointStream(additionalPermittedEndpoints)
                .map(this::fullyQualifiedClassNameToClass)
                .map(this::toEndpointId)
                .flatMap(Optional::stream)
                .map(this::toEndpointEnablingPropertyName)
                .collect(Collectors.toMap( Function.identity(), p -> (Object) "true"));
    }

    private Optional<String> toEndpointId(Class<?> endPointClass) {
        Optional<String> endpointId = ActuatorEndpointIdUtil.getEndpointId(endPointClass);
        if (endpointId.isEmpty()) {
            log.warn("Cannot derive endpoint id from alleged endpoint class {}. You will need to enable this endpoint on your own.", endPointClass.getName());
        }
        return endpointId;
    }

    private Stream<String> getAdditionalPermittedEndpointStream(String additionalPermittedEndpoints) {
        return Arrays.stream(additionalPermittedEndpoints.split("\\s*,\\s*"));
    }

    private Class<?> fullyQualifiedClassNameToClass(String fullyQualifiedClassName) {
        try {
            return Class.forName(fullyQualifiedClassName);
        } catch (ClassNotFoundException e){
            throw new IllegalArgumentException("Configured additional permitted endpoint class " + fullyQualifiedClassName + " is not on the class path.", e);
        }
    }

    private String toEndpointEnablingPropertyName(String endpointId) {
        return String.format(ENABLE_ENDPOINT_BY_ID_TEMPLATE, endpointId);
    }

    private void addPropertiesToEnvironment(Map<String, Object> properties, ConfigurableEnvironment environment) {
        MapPropertySource propertySource = new MapPropertySource("enable-additional-endpoints", properties);
        environment.getPropertySources().addLast(propertySource);
    }

}
