package ch.admin.bit.jeap.starter.db.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@AutoConfiguration
@Slf4j
@AllArgsConstructor
@ComponentScan
public class FlywayMigrationConfiguration {
    private final FlywayMigrationStrategyResolver flywayMigrationStrategyResolver;
    private final Environment environment;

    @Bean
    public FlywayMigrationStrategy customFlywayMigrateStrategy(ApplicationContext ctx) {
        return flyway -> {
            log.debug("Start custom flyway migration");
            flywayMigrationStrategyResolver.resolveFlywayStrategy(ctx, environment, flyway);
        };
    }
}

