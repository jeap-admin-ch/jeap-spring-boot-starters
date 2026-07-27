package ch.admin.bit.jeap.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootActuatorEndpointActivatorTest {

    private final SpringBootActuatorEndpointActivator activator = new SpringBootActuatorEndpointActivator();

    @Test
    void loadsActuatorDefaultsEarly() {
        StandardEnvironment environment = new StandardEnvironment();

        activator.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoints.enabled-by-default")).isEqualTo("false");
        assertThat(environment.getProperty("management.endpoint.health.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("management.endpoint.prometheus.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("management.tracing.propagation.produce")).isEqualTo("W3C,B3");
    }

    @Test
    void applicationConfigurationOverridesStarterDefaults() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "application-test",
                Map.of("management.endpoint.prometheus.enabled", "false")));

        activator.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.endpoint.prometheus.enabled")).isEqualTo("false");
    }
}
