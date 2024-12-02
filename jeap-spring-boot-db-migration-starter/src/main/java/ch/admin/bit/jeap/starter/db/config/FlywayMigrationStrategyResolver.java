package ch.admin.bit.jeap.starter.db.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Slf4j
@AllArgsConstructor
@Component
public class FlywayMigrationStrategyResolver {

    private final DatabaseMigrationProperties databaseMigrationProperties;
    private final FlywayMigrationStrategyService flywayMigrationStrategyService;

    public void resolveFlywayStrategy(ApplicationContext ctx, Environment environment, Flyway flyway) {
        if(CloudPlatform.KUBERNETES.isActive(environment)) {
            if (databaseMigrationProperties.isStartupMigrateModeEnabled()) {
                flywayMigrationStrategyService.executeStartupModeStrategy(flyway);
            } else if (databaseMigrationProperties.isInitContainer()) {
                flywayMigrationStrategyService.executeInitContainerStrategy(flyway);
            } else {
                flywayMigrationStrategyService.executeApplicationContainerStrategy(ctx, flyway);
            }
        } else {
            log.info("The application is not running on an Kubernetes platform, so start up migrate mode is enabled per default.");
            flywayMigrationStrategyService.executeStartupModeStrategy(flyway);
        }
    }
}
