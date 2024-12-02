package ch.admin.bit.jeap.log.syslog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@AutoConfigureObservability
@ActiveProfiles("cloud")
public class LoggingNoRollingFileIT extends SyslogIntegrationTestBase {

    private static final Path LOGFILE_PATH = Path.of("log.log");

    @Test
    @StdIo
    void when_rollingfileIsSetToFalseInCloudProfile_then_shouldLogToFileAndStdout(StdOut stdOut) throws IOException {
        String logMessage = "Some message logged using cloud profile to console and not to the file";
        log.info(logMessage);

        awaitAndAssertOneLogEntryOnStdOutContaining(logMessage, stdOut);

        assertFalse(Files.exists(LOGFILE_PATH));
    }
}
