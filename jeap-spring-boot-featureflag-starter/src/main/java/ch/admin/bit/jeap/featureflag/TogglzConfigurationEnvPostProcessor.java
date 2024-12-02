package ch.admin.bit.jeap.featureflag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class TogglzConfigurationEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = Map.of(
                "togglz.web.register-feature-interceptor", "false"
        );
        environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
    }

}
