package ch.admin.bit.jeap.featureflag;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class TogglzConfigurationEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        Map<String, Object> map = Map.of(
                "togglz.web.register-feature-interceptor", "false"
        );
        environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
    }

}
