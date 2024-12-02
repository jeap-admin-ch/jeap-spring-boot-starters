package ch.admin.bit.jeap.starter.db.config;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;

class PostMigrationCallbackTest {

    private static final int SHUTDOWN_STATUS_NORMAL = 0;
    private static final int SHUTDOWN_STATUS_ABNORMAL = 1;

    Context context = mock(Context.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    ShutdownService shutdownService = mock(ShutdownService.class);

    PostMigrationCallback cut;

    @Test
    void test_WhenIsInitContainerAndAfterMigrateEventIsCalled_ThenExitWithNormalStatus() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        migrationProperties.setStartupMigrateModeEnabled(false);
        migrationProperties.setInitContainer(true);

        cut = new PostMigrationCallback(applicationContext, shutdownService, migrationProperties);

        cut.supports(Event.AFTER_MIGRATE, context);

        verify(shutdownService).shutdown(applicationContext, SHUTDOWN_STATUS_NORMAL);
    }

    @Test
    void test_WhenIsInitContainerAndAfterMigrateEventIsCalled_ThenExitWithAbnormalStatus() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        migrationProperties.setStartupMigrateModeEnabled(false);
        migrationProperties.setInitContainer(true);

        cut = new PostMigrationCallback(applicationContext, shutdownService, migrationProperties);

        cut.supports(Event.AFTER_MIGRATE_ERROR, context);

        verify(shutdownService).shutdown(applicationContext, SHUTDOWN_STATUS_ABNORMAL);
    }

    @Test
    void test_WhenAppIsNotTheInitContainer_ThenDoNothing() {
        DatabaseMigrationProperties migrationProperties = new DatabaseMigrationProperties();
        migrationProperties.setStartupMigrateModeEnabled(false);
        migrationProperties.setInitContainer(false);

        cut = new PostMigrationCallback(applicationContext, shutdownService, migrationProperties);

        cut.supports(Event.AFTER_MIGRATE, context);

        verify(shutdownService, never()).shutdown(any(), anyInt());
    }
}
