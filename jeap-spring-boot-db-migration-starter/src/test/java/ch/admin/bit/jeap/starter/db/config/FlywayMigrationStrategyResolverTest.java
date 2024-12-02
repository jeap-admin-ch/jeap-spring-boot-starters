package ch.admin.bit.jeap.starter.db.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlywayMigrationStrategyResolverTest {

    Flyway flyway = mock(Flyway.class);
    ApplicationContext ctx = mock(ApplicationContext.class);
    Environment environment = mock(Environment.class);

    FlywayMigrationStrategyService flywayMigrationStrategyService = mock(FlywayMigrationStrategyService.class);
    FlywayMigrationStrategyResolver cut;

    @Test
    void test_WhenStartupMigrateModeIsEnabled_ThenExecuteStartupModeStrategy() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        migrationProperties.setStartupMigrateModeEnabled(true);
        when(environment.getProperty("spring.main.cloud-platform")).thenReturn("CLOUD_FOUNDRY");

        cut = new FlywayMigrationStrategyResolver(migrationProperties, flywayMigrationStrategyService);

        cut.resolveFlywayStrategy(ctx, environment, flyway);

        verify(flywayMigrationStrategyService).executeStartupModeStrategy(flyway);
    }

    @Test
    void test_WhenPlattformIsKubernetes_ThenExecuteStartupModeStrategy() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        when(environment.getProperty("spring.main.cloud-platform")).thenReturn("CLOUD_FOUNDRY");

        cut = new FlywayMigrationStrategyResolver(migrationProperties, flywayMigrationStrategyService);

        cut.resolveFlywayStrategy(ctx, environment, flyway);

        verify(flywayMigrationStrategyService).executeStartupModeStrategy(flyway);
    }

    @Test
    void test_WhenStartupMigrateModeIsEnabled_ThenExecuteInitContainerStrategy() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        when(environment.getProperty("spring.main.cloud-platform")).thenReturn("KUBERNETES");
        migrationProperties.setStartupMigrateModeEnabled(false);
        migrationProperties.setInitContainer(true);

        cut = new FlywayMigrationStrategyResolver(migrationProperties, flywayMigrationStrategyService);

        cut.resolveFlywayStrategy(ctx, environment, flyway);

        verify(flywayMigrationStrategyService).executeInitContainerStrategy(flyway);
    }

    @Test
    void test_WhenStartupMigrateModeIsEnabled_ThenExecuteApplicationContainerStrategy() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        when(environment.getProperty("spring.main.cloud-platform")).thenReturn("KUBERNETES");
        migrationProperties.setStartupMigrateModeEnabled(false);
        migrationProperties.setInitContainer(false);

        cut = new FlywayMigrationStrategyResolver(migrationProperties, flywayMigrationStrategyService);
        cut.resolveFlywayStrategy(ctx, environment, flyway);

        verify(flywayMigrationStrategyService).executeApplicationContainerStrategy(any(ApplicationContext.class), any(Flyway.class));
    }

}
