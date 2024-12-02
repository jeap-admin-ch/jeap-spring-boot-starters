package ch.admin.bit.jeap.starter.db.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


@Slf4j
@AllArgsConstructor
@Component
public class PostMigrationCallback implements Callback {

    private final ApplicationContext applicationContext;
    private final ShutdownService shutdownService;
    private final DatabaseMigrationProperties databaseMigrationProperties;

    @Override
    public boolean supports(Event event, Context context) {
        if(databaseMigrationProperties.isInitContainer() && !databaseMigrationProperties.isStartupMigrateModeEnabled()) {
            if (event == Event.AFTER_MIGRATE) {
                log.info("Flyway migration successful, exiting.");
                shutdownService.shutdown(applicationContext, 0);
            } else if (event == Event.AFTER_MIGRATE_ERROR) {
                log.error("Flyway migration failed, exiting.");
                shutdownService.shutdown(applicationContext, 1);
            }
        }
        return false;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        // This method is not used as the whole logic happens in the 'supports' method
    }

    @Override
    public String getCallbackName() {
        return PostMigrationCallback.class.getSimpleName();
    }
}
