package ch.admin.bit.jeap.starter.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * Sets sensible defaults for hikari DB connection pools. Otherwise, the hikari default of using a fixed-size
 * DB connection pool with 10 connections is applied. Applications can override pooling properties as they see fit,
 * according to <a href="https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby">the hikari docs</a>.
 * <p>
 * Uses the same values as <a href="https://github.com/spring-cloud/spring-cloud-connectors/blob/main/spring-cloud-spring-service-connector/src/main/java/org/springframework/cloud/service/relational/DataSourceConfigurer.java#L28">DataSourceConfigurer</a>
 * for defaults.
 */
public class DbPoolingDefaultsEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isHikariPresent()) {
            String poolNamePostfix = getPoolNamePostfix(environment);

            Map<String, Object> map = Map.of(
                    "spring.datasource.hikari.maximum-pool-size", "4",
                    "spring.datasource.hikari.minimum-idle", "0",
                    "spring.datasource.hikari.keepalive-time", "120000",
                    "spring.datasource.hikari.pool-name", "hikari-cp" + poolNamePostfix
            );
            environment.getPropertySources().addLast(new MapPropertySource(getClass().getSimpleName(), map));
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String getPoolNamePostfix(ConfigurableEnvironment environment) {
        String appName = environment.getProperty("spring.application.name", (String) null);
        return appName == null ? "" : "-" + appName;
    }

    private boolean isHikariPresent() {
        return ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource",
                ClassUtils.getDefaultClassLoader());
    }
}

