package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@AutoConfigureObservability
@ActiveProfiles({"cloud", "rollingLogFile"})
public class LogMessageFilterIT extends SyslogIntegrationTestBase {

    private static final Path LOGFILE_PATH = Path.of("log.log");

    @Test
    @StdIo
    void filteredMessage_should_not_log(StdOut stdOut) throws IOException {
        String logMessage = "Some message which should be logged";
        String logMessage_2 = "Some message which should be logged, too";
        String shouldNotLogMessage = "---Found no committed offset for partition....";
        log.info(logMessage);
        log.info(shouldNotLogMessage);
        log.info(logMessage_2);

        awaitAndAssertAtLeastOneJsonLogEntryOnStdOutContaining(logMessage_2, stdOut);

        assertTrue(Files.exists(LOGFILE_PATH));
        String logfileContents = Files.readString(LOGFILE_PATH);
        assertTrue(logfileContents.contains(logMessage));
        assertFalse(logfileContents.contains(shouldNotLogMessage));
        assertTrue(logfileContents.contains(logMessage_2));

    }

    @AfterAll
    static void deleteLogFile() {
        try {
            Files.deleteIfExists(LOGFILE_PATH);
        } catch (IOException ignored) {
        }
    }
}
