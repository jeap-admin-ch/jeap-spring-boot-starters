package ch.admin.bit.jeap.starter.db.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "database-migration")
public class DatabaseMigrationProperties {

    /**
     * Env variable that indicates the application being started in an init container or a normal one.
     * The flyway migration should only occur in the init container.
     */
    private boolean isInitContainer = false;

    /**
     * Env variable that if set, will force the application startup behavior of calling flyway.migrate on every startup
     */
    private boolean startupMigrateModeEnabled = false;
}
