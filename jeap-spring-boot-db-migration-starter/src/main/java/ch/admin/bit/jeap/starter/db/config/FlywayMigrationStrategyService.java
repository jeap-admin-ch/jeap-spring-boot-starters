package ch.admin.bit.jeap.starter.db.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.InfoOutput;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


@Slf4j
@AllArgsConstructor
@Component
public class FlywayMigrationStrategyService {

    private ShutdownService shutdownService;

    public void executeStartupModeStrategy(Flyway flyway) {
        log.debug("Running in startupMigrateMode, starting migration and continuing after");
        flyway.migrate();
    }

    public void executeInitContainerStrategy(Flyway flyway) {
        log.debug("Running in InitContainer, starting migration and exiting after");
        //The result is caught in the PostMigrationCallback.class
        flyway.migrate();
    }

    public void executeApplicationContainerStrategy(ApplicationContext ctx, Flyway flyway) {
        MigrationInfoService info = flyway.info();
        log.debug(MigrationInfoDumper.dumpToAsciiTable(info.all()));
        var pending = false;
        for (var i = 0; i < info.getInfoResult().migrations.size(); i++) {
            InfoOutput pendingMigration = info.getInfoResult().migrations.get(i);
            if (
                    "VERSIONED".equals(pendingMigration.category) &&
                            (pendingMigration.state.contains("FAILED") || "PENDING".equals(pendingMigration.state))
            ) {
                pending = true;
                break;
            }
        }
        if (!pending) {
            //If everything has been migrated from the init container, the application can start
            log.debug("No Migrations needed, continue startup");
        } else {
            //If the application container determines that migrations that should actually be carried out by the init container are still missing, it is terminated
            log.error("Found pending migrations, startup will be cancelled");
            shutdownService.shutdown(ctx, 1);
        }
    }
}
