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

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@AutoConfigureObservability
@ActiveProfiles({"aws", "rollingLogFile"})
public class LoggingRollingFileIT extends LogIntegrationTestBase {

    private static final Path LOGFILE_PATH = Path.of("log.log");

    @Test
    @StdIo
    void when_rollingfileIsSetInAwsProfile_then_shouldLogToFileAndStdout(StdOut stdOut) throws IOException {
        String logMessage = "Some message logged using cloud profile to console and file";
        log.info(logMessage);

        awaitAndAssertOneLogEntryOnStdOutContaining(logMessage, stdOut);

        assertTrue(Files.exists(LOGFILE_PATH));
        String logfileContents = Files.readString(LOGFILE_PATH);
        assertTrue(logfileContents.contains(logMessage));
    }

    @AfterAll
    static void deleteLogFile() {
        try {
            Files.deleteIfExists(LOGFILE_PATH);
        } catch (IOException ignored) {
        }
    }
}
